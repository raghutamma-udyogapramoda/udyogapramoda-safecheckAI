package com.safecheck.android.data.threat

import com.safecheck.android.data.api.dto.EvidenceDto

data class ThreatIntelResult(
    val providerName: String,
    val isMalicious: Boolean,
    val confidence: Double = 0.0,
    val threatType: String? = null,
    val evidence: List<EvidenceDto> = emptyList(),
    val isAvailable: Boolean = true,
    val failureReason: String? = null,
)

interface ThreatIntelligenceProvider {
    val providerName: String
    val requiresApiKey: Boolean
    suspend fun check(url: String, domain: String): ThreatIntelResult
}
