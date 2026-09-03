package com.safecheck.android.data.api

import com.safecheck.android.data.api.dto.CheckRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the deterministic mock's guarantees (requirements R-6.2.2, R-11):
 * evidence sub-engine points always sum to the displayed score, and the reference HIGH
 * scenario returns 87/100 HIGH.
 */
class MockSafeCheckApiTest {

    private val api = MockSafeCheckApi()

    @Test
    fun highScenario_is87_and_HIGH() = runTest {
        val resp = api.check(
            CheckRequest(
                inputType = "sms",
                content = "your account will be blocked, complete kyc and pay fee at http://sbi-kyc-update.xyz",
                sourceType = "sms_demo",
            )
        )
        assertEquals(87, resp.riskScore)
        assertEquals("HIGH", resp.riskLevel)
    }

    @Test
    fun subEngineSum_equalsScore_forHigh() = runTest {
        val resp = api.check(CheckRequest("sms", "account blocked pay fee http://x.xyz", "sms_demo"))
        val evidenceSum = resp.evidence.sumOf { it.points }
        val subSum = resp.subScores.mlPts + resp.subScores.urlPts + resp.subScores.rulePts
        assertEquals(resp.riskScore, subSum)
        assertEquals(resp.riskScore, evidenceSum)
    }

    @Test
    fun safeMessage_isLow() = runTest {
        val resp = api.check(CheckRequest("text", "Your parcel will arrive tomorrow between 9 and 11 am.", "manual"))
        assertEquals("LOW", resp.riskLevel)
        assertTrue(resp.riskScore < 40)
    }

    @Test
    fun suspiciousUrl_reportsUnavailableSignalHonestly() = runTest {
        val resp = api.check(CheckRequest("url", "http://secure-login-paypa1.xyz", "manual"))
        assertTrue(resp.unavailableSignals.isNotEmpty())
        val subSum = resp.subScores.mlPts + resp.subScores.urlPts + resp.subScores.rulePts
        assertEquals(resp.riskScore, subSum)
    }
}
