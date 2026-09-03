package com.safecheck.android.data.threat

import com.safecheck.android.data.api.dto.EvidenceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class UrlhausProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
) : ThreatIntelligenceProvider {

    override val providerName: String = "URLhaus"
    override val requiresApiKey: Boolean = false

    override suspend fun check(url: String, domain: String): ThreatIntelResult = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("url", url)
                .build()

            val request = Request.Builder()
                .url("https://urlhaus-api.abuse.ch/v1/url/")
                .post(formBody)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code in 400..499) {
                        return@withContext ThreatIntelResult(providerName, isMalicious = false, isAvailable = true)
                    }
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = false,
                        isAvailable = false,
                        failureReason = "HTTP ${response.code}",
                    )
                }

                val bodyStr = response.body?.string() ?: return@withContext ThreatIntelResult(
                    providerName = providerName,
                    isMalicious = false,
                    isAvailable = false,
                )

                val json = JSONObject(bodyStr)
                val status = json.optString("query_status")
                if (status == "ok") {
                    val threat = json.optString("threat", "malware_download")
                    val evidence = listOf(
                        EvidenceDto(
                            evidenceId = "ev_urlhaus_" + UUID.randomUUID().toString().take(6),
                            subEngine = "url",
                            label = "URLhaus Confirmed Malicious Distribution Flag",
                            points = 30,
                            observedValue = "Active threat payload: $threat",
                            confidence = 0.99,
                            correlationGroup = "CORR_THREAT_INTEL",
                            source = "URLhaus",
                            severity = "CRITICAL",
                        )
                    )
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = true,
                        confidence = 0.99,
                        threatType = threat,
                        evidence = evidence,
                    )
                } else {
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = false,
                        confidence = 0.0,
                    )
                }
            }
        } catch (e: Exception) {
            ThreatIntelResult(
                providerName = providerName,
                isMalicious = false,
                isAvailable = false,
                failureReason = e.message ?: "Network timeout",
            )
        }
    }
}
