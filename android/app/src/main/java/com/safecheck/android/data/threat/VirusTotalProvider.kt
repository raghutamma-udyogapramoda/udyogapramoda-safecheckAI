package com.safecheck.android.data.threat

import com.safecheck.android.BuildConfig
import com.safecheck.android.data.api.dto.EvidenceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

class VirusTotalProvider(
    private val apiKey: String = BuildConfig.VIRUSTOTAL_API_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) : ThreatIntelligenceProvider {

    override val providerName: String = "VirusTotal"
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
            val normalizedUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                "https://$url"
            } else {
                url
            }

            val urlId = Base64.getUrlEncoder().withoutPadding().encodeToString(normalizedUrl.toByteArray(Charsets.UTF_8))

            val request = Request.Builder()
                .url("https://www.virustotal.com/api/v3/urls/$urlId")
                .header("x-apikey", apiKey)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = false,
                        isAvailable = false,
                        failureReason = "API quota exceeded (rate limit active)",
                    )
                }

                if (response.code == 404) {
                    // URL not yet scanned in VirusTotal database; check domain reputation
                    if (domain.isNotBlank()) {
                        return@withContext checkDomain(domain)
                    }
                    return@withContext ThreatIntelResult(providerName, false)
                }

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
                val data = json.optJSONObject("data") ?: return@withContext ThreatIntelResult(providerName, false)
                val attributes = data.optJSONObject("attributes") ?: return@withContext ThreatIntelResult(providerName, false)
                val stats = attributes.optJSONObject("last_analysis_stats")

                val maliciousCount = stats?.optInt("malicious", 0) ?: 0
                val suspiciousCount = stats?.optInt("suspicious", 0) ?: 0

                if (maliciousCount >= 1 || (maliciousCount + suspiciousCount) >= 2) {
                    val evidence = listOf(
                        EvidenceDto(
                            evidenceId = "ev_vt_" + UUID.randomUUID().toString().take(6),
                            subEngine = "url",
                            label = "VirusTotal Multi-Engine Malicious Flag ($maliciousCount engines)",
                            points = 25,
                            observedValue = "$maliciousCount security vendors flagged as malicious",
                            confidence = 0.95,
                            correlationGroup = "CORR_THREAT_INTEL",
                            source = "VirusTotal",
                            severity = "HIGH",
                        )
                    )
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = true,
                        confidence = 0.95,
                        threatType = "multivendor_malicious_detection",
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

    private fun checkDomain(domain: String): ThreatIntelResult {
        return try {
            val req = Request.Builder()
                .url("https://www.virustotal.com/api/v3/domains/$domain")
                .header("x-apikey", apiKey)
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return ThreatIntelResult(providerName, false)
                }
                val body = resp.body?.string() ?: return ThreatIntelResult(providerName, false)
                val json = JSONObject(body)
                val stats = json.optJSONObject("data")?.optJSONObject("attributes")?.optJSONObject("last_analysis_stats")
                val malicious = stats?.optInt("malicious", 0) ?: 0
                val suspicious = stats?.optInt("suspicious", 0) ?: 0

                if (malicious >= 1 || (malicious + suspicious) >= 2) {
                    ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = true,
                        confidence = 0.95,
                        threatType = "domain_reputation_flagged",
                        evidence = listOf(
                            EvidenceDto(
                                evidenceId = "ev_vt_" + UUID.randomUUID().toString().take(6),
                                subEngine = "url",
                                label = "VirusTotal Malicious Domain ($malicious vendor flags)",
                                points = 25,
                                observedValue = "$domain flagged by $malicious security vendors",
                                confidence = 0.95,
                                correlationGroup = "CORR_THREAT_INTEL",
                                source = "VirusTotal",
                                severity = "HIGH",
                            )
                        )
                    )
                } else {
                    ThreatIntelResult(providerName, false)
                }
            }
        } catch (_: Exception) {
            ThreatIntelResult(providerName, false)
        }
    }
}
