package com.safecheck.android

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class CheckType(val label: String) { SMS("SMS"), TEXT("Text"), EMAIL("Email"), URL("URL"), IMAGE("Image"), VIDEO("Video") }
data class AnalysisResult(val verdict: String, val summary: String, val evidence: List<String> = emptyList(), val confidence: Double? = null)

/** Live reputation client. It never follows or opens submitted URLs. */
class ThreatAnalyzer(private val context: Context) {
    private val urlPattern = Regex("(?i)\\b((?:https?://|www\\.)[^\\s<>()]+)")

    suspend fun analyzeText(type: CheckType, content: String, backend: String): AnalysisResult = withContext(Dispatchers.IO) {
        val links = if (type == CheckType.URL) listOf(content.trim()) else urlPattern.findAll(content).map { it.value }.toList()
        val findings = links.filter { it.isNotBlank() }.map { checkUrlhaus(normalize(it)) }
        val knownBad = findings.filter { it.first }
        if (knownBad.isNotEmpty()) return@withContext AnalysisResult("malicious", "Known malware URL detected. Do not open it or share credentials.", knownBad.map { it.second }, .95)
        if (backend.isNotBlank()) return@withContext requestTextBackend(type, content, backend, findings.map { it.second })
        if (links.isEmpty()) AnalysisResult("inconclusive", "No URL was found. Configure an analysis backend for SMS/email text classification.", listOf("URLhaus was not queried because no URL was present."))
        else AnalysisResult("inconclusive", "No URLhaus malware record was found. This is not proof that the message is safe.", findings.map { it.second })
    }

    suspend fun analyzeMedia(uri: Uri, type: CheckType, backend: String): AnalysisResult = withContext(Dispatchers.IO) {
        if (backend.isBlank()) return@withContext AnalysisResult("inconclusive", "Configure an HTTPS analysis backend before submitting media. No file was uploaded.")
        val c = connection(backend.trimEnd('/') + "/v1/analyze/media", "multipart/form-data; boundary=SafeCheckBoundary", 45_000)
        try {
            DataOutputStream(c.outputStream).use { out ->
                out.writeBytes("--SafeCheckBoundary\\r\\nContent-Disposition: form-data; name=\"type\"\\r\\n\\r\\n${type.name.lowercase()}\\r\\n")
                out.writeBytes("--SafeCheckBoundary\\r\\nContent-Disposition: form-data; name=\"file\"; filename=\"upload\"\\r\\nContent-Type: application/octet-stream\\r\\n\\r\\n")
                context.contentResolver.openInputStream(uri)?.use { it.copyTo(out) } ?: error("Unable to read selected file")
                out.writeBytes("\\r\\n--SafeCheckBoundary--\\r\\n")
            }; parse(c, emptyList())
        } finally { c.disconnect() }
    }

    private fun normalize(value: String) = if (value.startsWith("http", true)) value else "https://$value"
    private fun connection(address: String, contentType: String, timeout: Int) = (URL(address).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = timeout; setRequestProperty("Content-Type", contentType)
    }
    private fun checkUrlhaus(value: String): Pair<Boolean, String> {
        val c = connection("https://urlhaus-api.abuse.ch/v1/url/", "application/x-www-form-urlencoded", 15_000)
        c.setRequestProperty("User-Agent", "SafeCheckAI/1.0 (Android)")
        return try {
            c.outputStream.use { it.write("url=${URLEncoder.encode(value, "UTF-8")}".toByteArray()) }
            val json = JSONObject(readBody(c)); val found = json.optString("query_status") == "ok"
            found to if (found) "URLhaus: known ${json.optString("threat", "malware")} URL (${json.optString("url_status", "reported")})." else "URLhaus: no known malware record for $value."
        } catch (e: Exception) { false to "URLhaus could not be reached: ${e.message ?: "network error"}." } finally { c.disconnect() }
    }
    private fun requestTextBackend(type: CheckType, content: String, base: String, evidence: List<String>): AnalysisResult {
        val c = connection(base.trimEnd('/') + "/v1/analyze/text", "application/json", 30_000)
        return try { c.outputStream.use { it.write(JSONObject().put("type", type.name.lowercase()).put("content", content).toString().toByteArray()) }; parse(c, evidence) } finally { c.disconnect() }
    }
    private fun readBody(c: HttpURLConnection): String { val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream; return BufferedReader(InputStreamReader(stream)).readText() }
    private fun parse(c: HttpURLConnection, evidence: List<String>): AnalysisResult { val json = JSONObject(readBody(c)); return AnalysisResult(json.optString("verdict", "inconclusive"), json.optString("summary", "Analysis completed."), evidence, json.optDouble("confidence").takeIf { !it.isNaN() }) }
}
