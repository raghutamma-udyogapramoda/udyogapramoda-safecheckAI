package com.safecheck.android.domain.analysis

import com.safecheck.android.domain.analysis.entity.BankDetector
import com.safecheck.android.domain.analysis.entity.CourierDetector
import com.safecheck.android.domain.analysis.entity.CredentialDetector
import com.safecheck.android.domain.analysis.entity.EntityCategory
import com.safecheck.android.domain.analysis.entity.EntityDetector
import com.safecheck.android.domain.analysis.entity.ExtractedEntity
import com.safecheck.android.domain.analysis.entity.GovernmentDetector
import com.safecheck.android.domain.analysis.entity.PaymentDetector
import com.safecheck.android.domain.analysis.entity.RemoteAccessDetector
import com.safecheck.android.domain.analysis.entity.SocialEngineeringDetector

data class ExtractedEntities(
    val banks: List<String> = emptyList(),
    val couriers: List<String> = emptyList(),
    val government: List<String> = emptyList(),
    val paymentApps: List<String> = emptyList(),
    val remoteTools: List<String> = emptyList(),
    val amounts: List<String> = emptyList(),
    val upiIds: List<String> = emptyList(),
    val hasApkMention: Boolean = false,
    val hasOtpAsk: Boolean = false,
    val hasPinAsk: Boolean = false,
    val hasPasswordAsk: Boolean = false,
    val hasPanDemand: Boolean = false,
    val hasUrgentThreat: Boolean = false,
    val hasSuspiciousAction: Boolean = false,
    val rawEntities: List<ExtractedEntity> = emptyList(),
)

/**
 * Composite entity extraction engine executing modular detectors.
 */
object EntityExtractor {

    private val detectors: List<EntityDetector> = listOf(
        BankDetector(),
        GovernmentDetector(),
        CourierDetector(),
        RemoteAccessDetector(),
        CredentialDetector(),
        PaymentDetector(),
        SocialEngineeringDetector(),
    )

    fun extract(text: String): ExtractedEntities {
        val allEntities = detectors.flatMap { it.detect(text) }

        val banks = allEntities.filter { it.category == EntityCategory.BANK }.map { it.label }.distinct()
        val couriers = allEntities.filter { it.category == EntityCategory.COURIER }.map { it.label }.distinct()
        val gov = allEntities.filter { it.category == EntityCategory.GOVERNMENT }.map { it.label }.distinct()
        val remoteTools = allEntities.filter { it.category == EntityCategory.REMOTE_TOOL }.map { it.value.lowercase() }.distinct()
        val paymentApps = allEntities.filter { it.category == EntityCategory.PAYMENT && it.label == "Payment App Mention" }.map { it.value }.distinct()
        val amounts = allEntities.filter { it.category == EntityCategory.PAYMENT && it.label == "Currency Amount" }.map { it.value }.distinct()
        val upis = allEntities.filter { it.category == EntityCategory.PAYMENT && it.label == "UPI VPA ID" }.map { it.value }.distinct()

        val hasApk = allEntities.any { it.label.contains("APK", ignoreCase = true) }
        val hasOtp = allEntities.any { it.label.contains("OTP", ignoreCase = true) }
        val hasPin = allEntities.any { it.label.contains("PIN", ignoreCase = true) }
        val hasPassword = allEntities.any { it.label.contains("Password", ignoreCase = true) }
        val hasPan = allEntities.any { it.label.contains("KYC", ignoreCase = true) || it.label.contains("PAN", ignoreCase = true) }
        val hasUrgent = allEntities.any { it.label.contains("Urgency", ignoreCase = true) }

        val hasSuspiciousAction = hasOtp || hasPin || hasPassword || hasApk || (hasPan && hasUrgent) || remoteTools.isNotEmpty()

        return ExtractedEntities(
            banks = banks,
            couriers = couriers,
            government = gov,
            paymentApps = paymentApps,
            remoteTools = remoteTools,
            amounts = amounts,
            upiIds = upis,
            hasApkMention = hasApk,
            hasOtpAsk = hasOtp,
            hasPinAsk = hasPin,
            hasPasswordAsk = hasPassword,
            hasPanDemand = hasPan,
            hasUrgentThreat = hasUrgent,
            hasSuspiciousAction = hasSuspiciousAction,
            rawEntities = allEntities,
        )
    }
}
