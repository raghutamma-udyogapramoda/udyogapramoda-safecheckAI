package com.safecheck.android.domain.analysis

import com.safecheck.android.data.api.MockSafeCheckApi
import com.safecheck.android.data.api.dto.CheckRequest
import com.safecheck.android.domain.analysis.ml.OnDeviceScamClassifier
import com.safecheck.android.domain.redaction.RedactionEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive unit tests for SafeCheck P1/P2 industrial intelligence components:
 * 1. Redaction of sensitive user credentials (OTP, PAN, Aadhaar, PIN, passwords, cards)
 * 2. Modular Entity Extraction (Banks, Couriers, Government, Remote Access, Credentials, Payments)
 * 3. Hardened Domain Intelligence (PSL parsing, Levenshtein distance, IP detection, TLD hazards, shorteners)
 * 4. Sender & Context Intelligence (impersonation, foreign mismatch, contact behavioral anomaly)
 * 5. Deterministic Risk Engine & Mathematical Proof (min(100, ML + URL + Rules))
 * 6. Non-double-counting correlation groups
 * 7. Real on-device statistical NLP classifier
 * 8. Safe baseline guarantees for benign personal & transactional communication
 */
class IndustryIntelligenceTest {

    private val redactionEngine = RedactionEngine()
    private val api = MockSafeCheckApi()

    // --- 1. REDACTION TESTS ---
    @Test
    fun redaction_masksSensitiveCredentials() {
        val input = "Your OTP is 492018 and PAN is ABCDE1234F. Password: mysecretpassword"
        val result = redactionEngine.redact(input)

        assertFalse(result.maskedText.contains("492018"))
        assertFalse(result.maskedText.contains("ABCDE1234F"))
        assertFalse(result.maskedText.contains("mysecretpassword"))
        assertTrue(result.hits.isNotEmpty())
    }

    // --- 2. MODULAR ENTITY EXTRACTION TESTS ---
    @Test
    fun entityExtractor_detectsRemoteToolsAndCredentials() {
        val input = "Please install AnyDesk to verify your pending e-challan and provide your UPI PIN."
        val entities = EntityExtractor.extract(input)

        assertTrue("Remote tool should be detected", entities.remoteTools.contains("anydesk"))
        assertTrue("Government/Police should be detected", entities.government.any { it.contains("Challan") })
        assertTrue("PIN request should be detected", entities.hasPinAsk)
        assertTrue("Suspicious action flag should be true", entities.hasSuspiciousAction)
    }

    @Test
    fun entityExtractor_detectsCourierAndIndianBanks() {
        val input = "Your India Post consignment from State Bank of India is held. Update KYC immediately."
        val entities = EntityExtractor.extract(input)

        assertTrue(entities.couriers.contains("India Post"))
        assertTrue(entities.banks.any { it.contains("State Bank of India") })
        assertTrue(entities.hasPanDemand)
        assertTrue(entities.hasUrgentThreat)
    }

    // --- 3. DOMAIN INTELLIGENCE TESTS ---
    @Test
    fun domainIntelligence_detectsLookalikeAndHighRiskTld() {
        val text = "Login here: https://sbi-kyc-update.xyz/login"
        val result = DomainIntelligenceEngine.analyze(text)

        assertTrue("Should detect lookalike", result.isLookalike)
        assertTrue("Should contain evidence", result.evidence.isNotEmpty())
        assertTrue("Points should be > 0", result.totalUrlPts > 0)
        assertTrue("Points must be capped at 30", result.totalUrlPts <= 30)
    }

    @Test
    fun domainIntelligence_detectsRawIpAddress() {
        val text = "Access your funds at http://192.168.1.100/admin"
        val result = DomainIntelligenceEngine.analyze(text)

        assertTrue(result.evidence.any { it.label.contains("Raw IP") })
    }

    @Test
    fun domainIntelligence_doesNotFlagLegitimateOfficialDomain() {
        val text = "Visit official banking portal at https://retail.onlinesbi.sbi/retail/login.htm"
        val result = DomainIntelligenceEngine.analyze(text)

        assertFalse("Official SBI subdomain should NOT be flagged as lookalike", result.isLookalike)
    }

    @Test
    fun domainIntelligence_falsePositiveResistanceOnSubstring() {
        // "transbian" contains "sbi" as an internal substring, but is NOT an SBI lookalike!
        val text = "Check the article at https://transbian.org/about"
        val result = DomainIntelligenceEngine.analyze(text)

        assertFalse("Arbitrary substring match must NOT flag lookalike", result.isLookalike)
    }

    // --- 4. SENDER INTELLIGENCE TESTS ---
    @Test
    fun senderIntelligence_detectsInstitutionImpersonationFromPersonalMobile() {
        val entities = EntityExtractor.extract("Your HDFC Bank account is blocked today.")
        val result = SenderIntelligenceEngine.evaluate(sender = "+919876543210", entities = entities)

        assertTrue("Should flag personal mobile claiming to be official institution", result.evidence.any { it.label.contains("Personal Sender") })
        assertTrue("Points should be allocated", result.points > 0)
    }

    // --- 5. ON-DEVICE STATISTICAL ML MODEL TESTS ---
    @Test
    fun mlModel_detectsUrgentFinancialScam() {
        val text = "Dear customer, your electricity power will be disconnection tonight. Pay pending bill fee immediately or arrest warrant will be issued."
        val signal = OnDeviceScamClassifier.predict(text)

        assertTrue("Should classify as scam pattern", signal.isScam)
        assertTrue("Probability should be high", signal.probability >= 0.70)
        assertTrue("Points should be in upper range", signal.points >= 35)
        assertNotNull(signal.evidence)
    }

    @Test
    fun mlModel_scoresBenignMessageAsLowRisk() {
        val text = "Hey, let us meet for dinner at 8pm. Thanks!"
        val signal = OnDeviceScamClassifier.predict(text)

        assertFalse("Benign text should not be flagged as scam", signal.isScam)
        assertTrue("Probability should be low", signal.probability < 0.30)
        assertEquals(6, signal.points) // Safe baseline
    }

    // --- 6. UNIFIED DETERMINISTIC RISK ENGINE TESTS ---
    @Test
    fun deterministicEngine_exactMathematicalProof() = runTest {
        val request = CheckRequest(
            inputType = "sms",
            content = "Your SBI account will be blocked today. Verify KYC and pay Rs 50 unblock fee at https://sbi-kyc-update.xyz",
            sourceType = "sms_real",
            sender = "+919876543210",
        )
        val response = api.check(request)

        val subSum = response.subScores.mlPts + response.subScores.urlPts + response.subScores.rulePts
        assertEquals("Sub-scores must equal total score", response.riskScore, minOf(100, subSum))
        assertEquals("HIGH", response.riskLevel)
        assertTrue("Risk score should be high for this multi-signal threat", response.riskScore >= 75)
    }

    @Test
    fun deterministicEngine_safeMessage_returnsLowWithBaseline() = runTest {
        val request = CheckRequest(
            inputType = "text",
            content = "Thanks for the meeting today, let's catch up tomorrow lunch.",
            sourceType = "manual",
        )
        val response = api.check(request)

        assertEquals("LOW", response.riskLevel)
        assertEquals(12, response.riskScore) // 6 ML + 6 Rule = 12 Safe Baseline
        assertEquals(6, response.subScores.mlPts)
        assertEquals(0, response.subScores.urlPts)
        assertEquals(6, response.subScores.rulePts)
    }

    @Test
    fun deterministicEngine_identicalInput_givesIdenticalScore() = runTest {
        val req1 = CheckRequest("sms", "Your parcel is pending. Verify at http://track-package.top", "sms_demo")
        val req2 = CheckRequest("sms", "Your parcel is pending. Verify at http://track-package.top", "sms_demo")

        val res1 = api.check(req1)
        val res2 = api.check(req2)

        assertEquals("Scores must be identical for identical inputs", res1.riskScore, res2.riskScore)
        assertEquals("Bands must be identical", res1.riskLevel, res2.riskLevel)
        assertEquals("ML points must be identical", res1.subScores.mlPts, res2.subScores.mlPts)
        assertEquals("URL points must be identical", res1.subScores.urlPts, res2.subScores.urlPts)
        assertEquals("Rule points must be identical", res1.subScores.rulePts, res2.subScores.rulePts)
    }
}
