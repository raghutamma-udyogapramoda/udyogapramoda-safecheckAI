package com.safecheck.android.domain.analysis.explanation

import com.safecheck.android.BuildConfig
import com.safecheck.android.data.api.dto.EvidenceDto
import com.safecheck.android.domain.analysis.ExplanationEngine
import com.safecheck.android.domain.analysis.ExtractedEntities
import com.safecheck.android.domain.analysis.UrlAnalysisResult
import com.safecheck.android.ui.theme.RiskBand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface ExplanationProvider {
    val providerName: String
    suspend fun generate(
        score: Int,
        band: RiskBand,
        entities: ExtractedEntities,
        urlResult: UrlAnalysisResult,
        evidence: List<EvidenceDto>,
        sender: String?,
        language: ExplanationEngine.Language,
    ): ExplanationEngine.ExplanationResult
}

class LocalTemplateExplanationProvider : ExplanationProvider {
    override val providerName: String = "LocalTemplate"

    override suspend fun generate(
        score: Int,
        band: RiskBand,
        entities: ExtractedEntities,
        urlResult: UrlAnalysisResult,
        evidence: List<EvidenceDto>,
        sender: String?,
        language: ExplanationEngine.Language,
    ): ExplanationEngine.ExplanationResult {
        return ExplanationEngine.generate(
            score = score,
            band = band,
            entities = entities,
            urlResult = urlResult,
            sender = sender,
            language = language,
        )
    }
}

/**
 * Downstream explanation provider calling Google Gemini via OpenRouter.
 * Strict Constraint: Generates reasoning text only. It NEVER modifies the risk score or invents unobserved facts.
 * Falls back to LocalTemplateExplanationProvider on timeout or missing API key.
 */
class OpenRouterGeminiExplanationProvider(
    private val apiKey: String = BuildConfig.OPENROUTER_API_KEY,
    private val localFallback: LocalTemplateExplanationProvider = LocalTemplateExplanationProvider(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build(),
) : ExplanationProvider {

    override val providerName: String = "OpenRouterGemini"

    override suspend fun generate(
        score: Int,
        band: RiskBand,
        entities: ExtractedEntities,
        urlResult: UrlAnalysisResult,
        evidence: List<EvidenceDto>,
        sender: String?,
        language: ExplanationEngine.Language,
    ): ExplanationEngine.ExplanationResult = withContext(Dispatchers.IO) {
        val baseResult = localFallback.generate(score, band, entities, urlResult, evidence, sender, language)

        if (apiKey.isBlank()) {
            return@withContext baseResult
        }

        try {
            val evidenceSummary = evidence.joinToString("; ") { "${it.label}: ${it.observedValue ?: "detected"}" }
            val langPrompt = if (language == ExplanationEngine.Language.HINDI) "Hindi (Devanagari script)" else "English"

            val prompt = """
                You are the SafeCheck Cyber Copilot Explanation Assistant.
                A security analysis was already deterministically completed with Score: $score/100, Band: ${band.label}.
                Observed Evidence: $evidenceSummary.
                Sender: ${sender ?: "Unknown"}.
                
                Instruction:
                Write a concise 2-sentence explanation in $langPrompt explaining what was detected and why it is suspicious based ONLY on the evidence above.
                Do NOT mention any numbers, points, or scores. Do NOT speculate or invent new facts.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", "google/gemini-2.5-flash")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 120)
                put("temperature", 0.2)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://safecheck.local")
                .header("X-Title", "SafeCheck Android")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext baseResult
                }

                val bodyStr = response.body?.string() ?: return@withContext baseResult
                val respJson = JSONObject(bodyStr)
                val choices = respJson.optJSONArray("choices") ?: return@withContext baseResult
                if (choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    val content = message?.optString("content")?.trim()

                    if (!content.isNullOrBlank()) {
                        return@withContext baseResult.copy(
                            narrative = content
                        )
                    }
                }
            }
            baseResult
        } catch (e: Exception) {
            // Gracefully fall back to local templates without disruption
            baseResult
        }
    }
}
