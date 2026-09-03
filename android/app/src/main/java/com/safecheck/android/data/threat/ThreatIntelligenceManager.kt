package com.safecheck.android.data.threat

import com.safecheck.android.data.api.dto.EvidenceDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

data class ThreatIntelSummary(
    val evidence: List<EvidenceDto> = emptyList(),
    val unavailableSignals: List<String> = emptyList(),
)

/**
 * Orchestrates multi-source external threat intelligence (P1/P2 architecture).
 * Caches lookups by normalized indicator to avoid redundant network calls and quota exhaustion.
 * Enforces correlation group fusion to prevent double-counting multiple external hits.
 */
class ThreatIntelligenceManager(
    private val providers: List<ThreatIntelligenceProvider> = listOf(
        UrlhausProvider(),
        RdapProvider(),
        GoogleSafeBrowsingProvider(),
        VirusTotalProvider(),
    )
) {
    // In-memory LRU indicator cache: normalized URL -> ThreatIntelSummary
    private val cache = ConcurrentHashMap<String, ThreatIntelSummary>()

    suspend fun evaluate(urls: List<String>, domain: String?): ThreatIntelSummary = coroutineScope {
        if (urls.isEmpty() && domain.isNullOrBlank()) {
            return@coroutineScope ThreatIntelSummary()
        }

        val primaryUrl = urls.firstOrNull() ?: "https://$domain"
        val cleanDomain = domain ?: primaryUrl.substringBefore("/").substringAfter("://").substringBefore(":")

        // Check cache first
        cache[primaryUrl]?.let { return@coroutineScope it }

        val deferredResults = providers.map { provider ->
            async {
                provider.check(primaryUrl, cleanDomain)
            }
        }

        val results = deferredResults.map { it.await() }

        val combinedEvidence = mutableListOf<EvidenceDto>()
        val unavailable = mutableListOf<String>()

        // Anti-double-counting: Threat intelligence external hits are capped so multiple
        // malicious flags for the same URL fuse into the highest-confidence evidence.
        var externalThreatPoints = 0
        val maxExternalThreatPts = 30

        for (res in results) {
            if (!res.isAvailable && res.failureReason != null) {
                unavailable.add("${res.providerName} (${res.failureReason})")
            }

            if (res.evidence.isNotEmpty()) {
                for (ev in res.evidence) {
                    if (ev.correlationGroup == "CORR_THREAT_INTEL") {
                        if (externalThreatPoints < maxExternalThreatPts) {
                            val ptsToAdd = minOf(ev.points, maxExternalThreatPts - externalThreatPoints)
                            externalThreatPoints += ptsToAdd
                            combinedEvidence.add(ev.copy(points = ptsToAdd))
                        }
                    } else {
                        combinedEvidence.add(ev)
                    }
                }
            }
        }

        val summary = ThreatIntelSummary(
            evidence = combinedEvidence,
            unavailableSignals = unavailable.distinct(),
        )

        // Cache result (limit cache size to 100 entries)
        if (cache.size > 100) {
            cache.clear()
        }
        cache[primaryUrl] = summary

        summary
    }
}
