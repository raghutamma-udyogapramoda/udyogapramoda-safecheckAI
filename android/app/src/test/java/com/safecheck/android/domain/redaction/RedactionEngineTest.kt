package com.safecheck.android.domain.redaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the on-device redaction choke-point (requirements R-8.1, Master Spec §10).
 * The Master Spec masking examples MUST pass exactly.
 */
class RedactionEngineTest {

    private val engine = RedactionEngine()

    // --- Master Spec §10 exact examples ---

    @Test
    fun otp_123456_masksToStars() {
        assertEquals("******", engine.mask("123456"))
    }

    @Test
    fun card_grouped16_keepsFirst4AndLast4() {
        assertEquals("4111 **** **** 1111", engine.mask("4111 1111 1111 1111"))
    }

    @Test
    fun account_12digits_keepsLast4() {
        assertEquals("********9012", engine.mask("123456789012"))
    }

    // --- Labeled secrets ---

    @Test
    fun labeledOtp_isMasked_valueGone() {
        val out = engine.mask("Your OTP is 482913 do not share")
        assertFalse(out.contains("482913"))
        assertTrue(out.contains("******"))
    }

    @Test
    fun labeledPassword_isMasked() {
        val out = engine.mask("password: hunter2")
        assertFalse(out.contains("hunter2"))
    }

    @Test
    fun labeledPin_isMasked() {
        val out = engine.mask("your UPI PIN is 1234")
        assertFalse(out.contains("1234"))
    }

    // --- Structured Indian identifiers ---

    @Test
    fun ifsc_isFullyMasked() {
        val out = engine.mask("Branch IFSC SBIN0001234 today")
        assertFalse(out.contains("SBIN0001234"))
    }

    @Test
    fun pan_isFullyMasked() {
        val out = engine.mask("PAN ABCDE1234F on file")
        assertFalse(out.contains("ABCDE1234F"))
    }

    @Test
    fun aadhaar_12digitsGrouped_keepsLast4() {
        assertEquals("**** **** 9012", engine.mask("1234 5678 9012"))
    }

    @Test
    fun upiId_localPartMasked_handleKept() {
        val out = engine.redact("pay to john.doe@okhdfc")
        assertTrue(out.maskedText.contains("@okhdfc"))
        assertFalse(out.maskedText.contains("john.doe"))
        assertTrue(out.hits.any { it.type == PiiType.UPI_ID })
    }

    // --- Hit metadata carries type only, never the raw value ---

    @Test
    fun hits_recordTypeOnly_noRawValues() {
        val out = engine.redact("card 4111 1111 1111 1111 otp 123456")
        // Serialized hit names must not leak any digits.
        val serialized = out.hitTypeNames.joinToString(",")
        assertFalse(serialized.any { it.isDigit() })
        assertTrue(out.hits.any { it.type == PiiType.CARD_NUMBER })
    }

    // --- Safe content is left intact ---

    @Test
    fun plainSafeText_isUnchanged() {
        val safe = "Your package will be delivered tomorrow between 9 and 11 am."
        assertEquals(safe, engine.mask(safe))
    }

    @Test
    fun empty_returnsEmpty() {
        val out = engine.redact("")
        assertEquals("", out.maskedText)
        assertTrue(out.hits.isEmpty())
    }
}
