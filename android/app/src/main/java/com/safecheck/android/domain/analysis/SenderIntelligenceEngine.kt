package com.safecheck.android.domain.analysis

import com.safecheck.android.data.api.dto.EvidenceDto
import com.safecheck.android.domain.model.SenderContext
import com.safecheck.android.domain.model.TrustedContact
import java.util.UUID

data class SenderIntelligenceResult(
    val context: SenderContext,
    val evidence: List<EvidenceDto>,
    val points: Int,
)

/**
 * Evaluates sender origin, DLT header compliance, foreign geo-mismatch, and address book correlation.
 * Principle: Unknown sender != scam. Known sender != safe.
 */
object SenderIntelligenceEngine {

    // Indian commercial TRAI DLT header format: 2-char operator code + '-' + 6-char entity header
    private val DLT_HEADER_REGEX = Regex("^[A-Za-z]{2}-[A-Za-z]{6}$")

    // Standard Indian mobile number
    private val IN_MOBILE_REGEX = Regex("^(?:\\+91|91)?[6-9]\\d{9}$")

    fun evaluate(
        sender: String?,
        entities: ExtractedEntities,
        contacts: List<TrustedContact> = emptyList(),
    ): SenderIntelligenceResult {
        if (sender.isNullOrBlank()) {
            return SenderIntelligenceResult(
                context = SenderContext(senderId = "UNKNOWN"),
                evidence = emptyList(),
                points = 0,
            )
        }

        val cleanSender = sender.trim()
        val isDlt = DLT_HEADER_REGEX.matches(cleanSender)
        val isLocalMobile = IN_MOBILE_REGEX.matches(cleanSender.replace(" ", "").replace("-", ""))
        val isForeign = cleanSender.startsWith("+") && !cleanSender.startsWith("+91")

        val matchedContact = contacts.firstOrNull {
            it.phoneNumber.isNotBlank() && cleanSender.contains(it.phoneNumber.takeLast(10))
        }
        val isKnown = matchedContact != null

        val evidence = mutableListOf<EvidenceDto>()
        var points = 0

        // 1. Institution impersonation: Content claims to be a Bank/Gov, but sent from personal mobile number
        if ((entities.banks.isNotEmpty() || entities.government.isNotEmpty()) && isLocalMobile) {
            val target = (entities.banks + entities.government).first()
            val pts = 12
            points += pts
            evidence.add(
                EvidenceDto(
                    evidenceId = "ev_sender_" + UUID.randomUUID().toString().take(6),
                    subEngine = "rules",
                    label = "Suspicious Personal Sender for Official Entity",
                    points = pts,
                    observedValue = "Claims to be $target but sent from 10-digit mobile ($cleanSender)",
                    confidence = 0.88,
                    correlationGroup = "CORR_SENDER_IMPERSONATION",
                    source = "SenderIntelligence",
                    severity = "HIGH",
                )
            )
        }

        // 2. Foreign geo-mismatch: Foreign number messaging with Indian financial/government context
        if (isForeign && (entities.banks.isNotEmpty() || entities.government.isNotEmpty() || entities.upiIds.isNotEmpty())) {
            val pts = 15
            points += pts
            evidence.add(
                EvidenceDto(
                    evidenceId = "ev_sender_" + UUID.randomUUID().toString().take(6),
                    subEngine = "rules",
                    label = "Foreign Number Geo-Mismatch",
                    points = pts,
                    observedValue = "International sender code ($cleanSender) requesting Indian banking/govt action",
                    confidence = 0.90,
                    correlationGroup = "CORR_SENDER_IMPERSONATION",
                    source = "SenderIntelligence",
                    severity = "HIGH",
                )
            )
        }

        // 3. Compromised contact anomaly: Known contact suddenly demanding OTP or money
        if (isKnown && (entities.hasOtpAsk || entities.hasPinAsk || entities.amounts.isNotEmpty())) {
            val pts = 10
            points += pts
            evidence.add(
                EvidenceDto(
                    evidenceId = "ev_sender_" + UUID.randomUUID().toString().take(6),
                    subEngine = "rules",
                    label = "Compromised Account Behavioral Anomaly",
                    points = pts,
                    observedValue = "Trusted contact (${matchedContact?.name}) soliciting credentials or money transfer",
                    confidence = 0.80,
                    correlationGroup = "CORR_SENDER_ANOMALY",
                    source = "SenderIntelligence",
                    severity = "MEDIUM",
                )
            )
        }

        val senderContext = SenderContext(
            senderId = cleanSender,
            isKnownContact = isKnown,
            contactName = matchedContact?.name,
            isCommercialDltHeader = isDlt,
            isForeignNumber = isForeign,
            countryCode = if (isForeign) cleanSender.take(3) else "+91",
        )

        return SenderIntelligenceResult(
            context = senderContext,
            evidence = evidence,
            points = points.coerceAtMost(20),
        )
    }
}
