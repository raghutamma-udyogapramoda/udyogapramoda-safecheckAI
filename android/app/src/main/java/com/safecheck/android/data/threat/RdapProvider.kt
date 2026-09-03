package com.safecheck.android.data.threat

import com.safecheck.android.data.api.dto.EvidenceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class RdapProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
) : ThreatIntelligenceProvider {

    override val providerName: String = "RDAP"
    override val requiresApiKey: Boolean = false

    override suspend fun check(url: String, domain: String): ThreatIntelResult = withContext(Dispatchers.IO) {
        if (domain.isBlank() || domain.matches(Regex("^[0-9.]+$"))) {
            return@withContext ThreatIntelResult(providerName, false)
        }

        try {
            val request = Request.Builder()
                .url("https://rdap.org/domain/$domain")
                .header("Accept", "application/rdap+json, application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return@withContext ThreatIntelResult(providerName, isMalicious = false, isAvailable = true)
                    }
                    return@withContext ThreatIntelResult(
                        providerName = providerName,
                        isMalicious = false,
                        isAvailable = false,
                        failureReason = "HTTP ${response.code}",
                    )
                }

                val bodyStr = response.body?.string() ?: return@withContext ThreatIntelResult(providerName, false)
                val json = JSONObject(bodyStr)
                val events = json.optJSONArray("events") ?: return@withContext ThreatIntelResult(providerName, false)

                var registrationTimestamp: Long? = null
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

                for (i in 0 until events.length()) {
                    val ev = events.getJSONObject(i)
                    val action = ev.optString("eventAction")
                    if (action == "registration") {
                        val dateStr = ev.optString("eventDate")
                        registrationTimestamp = runCatching { sdf.parse(dateStr)?.time }.getOrNull()
                        break
                    }
                }

                if (registrationTimestamp != null) {
                    val ageMs = System.currentTimeMillis() - registrationTimestamp
                    val ageDays = (ageMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

                    if (ageDays < 30) {
                        val evidence = listOf(
                            EvidenceDto(
                                evidenceId = "ev_rdap_" + UUID.randomUUID().toString().take(6),
                                subEngine = "url",
                                label = "Newly-Registered Domain ($ageDays days old)",
                                points = 10,
                                observedValue = "$domain registered $ageDays days ago",
                                confidence = 0.85,
                                correlationGroup = "CORR_DOMAIN_AGE",
                                source = "RDAP",
                                severity = "MEDIUM",
                            )
                        )
                        return@withContext ThreatIntelResult(
                            providerName = providerName,
                            isMalicious = false,
                            confidence = 0.85,
                            threatType = "newly_registered_domain",
                            evidence = evidence,
                        )
                    }
                }
                return@withContext ThreatIntelResult(providerName, false)
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
