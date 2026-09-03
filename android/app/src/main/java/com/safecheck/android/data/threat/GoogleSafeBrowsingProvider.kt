package com.safecheck.android.data.threat

import com.safecheck.android.BuildConfig
import com.safecheck.android.data.api.dto.EvidenceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GoogleSafeBrowsingProvider(
    private val apiKey: String = BuildConfig.SAFE_BROWSING_API_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
) : ThreatIntelligenceProvider {

    override val providerName: String = "Google Safe Browsing"
    override val requiresApiKey: Boolean = true

    override suspend fun check(url: String, domain: String): ThreatIntelResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ThreatIntelResult(
                providerName = providerName,
                isMalicious = false,
                isAvailable = false,
                failureReason = "API key not configured (offline fallback active)",
            )
        }

        try {
            val targetUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                "https://$url"
            } else {
                url
            }

            val payload = JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientId", "com.safecheck.android")
                    put("clientVersion", "1.0.0")
                })
                put("threatInfo", JSONObject().apply {
                    put("threatTypes", JSONArray(listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION")))
                    put("platformTypes", JSONArray(listOf("ANY_PLATFORM")))
                    put("threatEntryTypes", JSONArray(listOf("URL")))
                    put("threatEntries", JSONArray(listOf(JSONObject().put("url", targetUrl))))
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$apiKey")
                .post(body)
                .header("X-Android-Package", "com.safecheck.android")
                .header("X-Android-Cert", "27369C153FD7E3B2BDC6186B7DEC66C983BE97DF")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = false,
                        isAvailable = false,
                        failureReason = "HTTP ${response.code}",
                    )
                }

                val bodyStr = response.body?.string() ?: return@withContext ThreatIntelResult(providerName, false)
                val json = JSONObject(bodyStr)
                val matches = json.optJSONArray("matches")

                if (matches != null && matches.length() > 0) {
                    val matchObj = matches.getJSONObject(0)
                    val threatType = matchObj.optString("threatType", "SOCIAL_ENGINEERING")

                    val evidence = listOf(
                        EvidenceDto(
                            evidenceId = "ev_gsb_" + UUID.randomUUID().toString().take(6),
                            subEngine = "url",
                            label = "Google Safe Browsing Malicious Threat Flag",
                            points = 30,
                            observedValue = "Classified as $threatType",
                            confidence = 0.99,
                            correlationGroup = "CORR_THREAT_INTEL",
                            source = "GoogleSafeBrowsing",
                            severity = "CRITICAL",
                        )
                    )

                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = true,
                        confidence = 0.99,
                        threatType = threatType,
                        evidence = evidence,
                    )
                } else {
                    return@withContext ThreatIntelResult(providerName, false)
                }
            }
        } catch (e: Exception) {
            ThreatIntelResult(
                providerName = providerName,
                isMalicious = false,
                isAvailable = false,
                failureReason = e.message ?: "Network error",
            )
        }
    }
}
