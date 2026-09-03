package com.safecheck.android.domain.redaction

/**
 * On-device PII redaction — the SINGLE privacy choke-point (design.md §3, requirements R-8.1).
 *
 * Every piece of outbound content (manual text, email body, OCR output, SMS body, PDF text)
 * MUST pass through [redact] before it is sent to the API, logged, or shown in a notification.
 * Sensitive values are masked at the source; only masked text and hit TYPES leave the device.
 *
 * This is a deterministic, rule-based masker (not an ML PII model). It is tuned to reliably
 * mask the Master Spec examples and the common Indian financial identifiers listed in §10:
 * OTP, card numbers, bank account numbers, IFSC, UPI IDs, PAN, Aadhaar, passwords, PINs.
 *
 * Ordering rationale: structured/high-specificity patterns (IFSC, PAN, UPI, Aadhaar, card)
 * are masked before generic digit runs (account, OTP) so a specific identifier is not first
 * consumed by a broad numeric rule.
 */
class RedactionEngine {

    private data class Rule(
        val type: PiiType,
        val regex: Regex,
        val mask: (MatchResult) -> String,
    )

    fun redact(input: String): RedactionResult {
        if (input.isEmpty()) return RedactionResult(input, emptyList())

        var text = input
        val hits = mutableListOf<RedactionHit>()

        for (rule in rules) {
            if (!rule.regex.containsMatchIn(text)) continue
            text = rule.regex.replace(text) { match ->
                hits.add(RedactionHit(rule.type))
                rule.mask(match)
            }
        }
        return RedactionResult(text, hits)
    }

    /** Convenience for callers that only need the masked string. */
    fun mask(input: String): String = redact(input).maskedText

    private companion object {
        /** Full mask of all non-space characters in [s]. */
        fun maskAll(s: String): String = s.map { if (it.isWhitespace()) it else '*' }.joinToString("")

        /** Keep the last [keep] digits, mask all earlier digits; preserve separators. */
        fun maskKeepLast(s: String, keep: Int): String {
            val digitIndices = s.indices.filter { s[it].isDigit() }
            val maskBefore = (digitIndices.size - keep).coerceAtLeast(0)
            val sb = StringBuilder()
            var digitSeen = 0
            for (ch in s) {
                if (ch.isDigit()) {
                    sb.append(if (digitSeen < maskBefore) '*' else ch)
                    digitSeen++
                } else sb.append(ch)
            }
            return sb.toString()
        }

        /** Keep first [first] and last [last] digits, mask the middle; preserve separators. */
        fun maskKeepEnds(s: String, first: Int, last: Int): String {
            val totalDigits = s.count { it.isDigit() }
            val sb = StringBuilder()
            var digitSeen = 0
            for (ch in s) {
                if (ch.isDigit()) {
                    val keep = digitSeen < first || digitSeen >= totalDigits - last
                    sb.append(if (keep) ch else '*')
                    digitSeen++
                } else sb.append(ch)
            }
            return sb.toString()
        }
    }

    private val rules: List<Rule> = listOf(
        // Passwords / PINs given with an explicit label, e.g. "password: hunter2", "PIN is 1234".
        Rule(
            type = PiiType.PASSWORD,
            regex = Regex("""(?i)\b(password|passwd|pwd)\b\s*(?:is|:|=)?\s*(\S+)"""),
            mask = { m -> m.value.replace(m.groupValues[2], maskAll(m.groupValues[2])) },
        ),
        Rule(
            type = PiiType.PIN,
            regex = Regex("""(?i)\b(pin|mpin|upi\s*pin)\b\s*(?:is|:|=)?\s*(\d{4,6})"""),
            mask = { m -> m.value.replace(m.groupValues[2], maskAll(m.groupValues[2])) },
        ),
        // OTP: labeled code, e.g. "OTP is 123456", "code: 4821".
        Rule(
            type = PiiType.OTP,
            regex = Regex("""(?i)\b(otp|one[\s-]?time[\s-]?password|verification\s*code|code)\b\s*(?:is|:|=)?\s*(\d{4,8})"""),
            mask = { m -> m.value.replace(m.groupValues[2], maskAll(m.groupValues[2])) },
        ),
        // IFSC: 4 letters + 0 + 6 alphanumerics.
        Rule(
            type = PiiType.IFSC,
            regex = Regex("""\b[A-Z]{4}0[A-Z0-9]{6}\b"""),
            mask = { m -> maskAll(m.value) },
        ),
        // PAN: 5 letters + 4 digits + 1 letter.
        Rule(
            type = PiiType.PAN,
            regex = Regex("""\b[A-Z]{5}[0-9]{4}[A-Z]\b"""),
            mask = { m -> maskAll(m.value) },
        ),
        // UPI ID / VPA: local@handle (mask the local part). Kept before generic numbers;
        // excludes email-looking values with a dot in the domain to reduce false positives.
        Rule(
            type = PiiType.UPI_ID,
            regex = Regex("""\b([\w.\-]{2,})@([a-zA-Z]{2,})\b"""),
            mask = { m -> maskAll(m.groupValues[1]) + "@" + m.groupValues[2] },
        ),
        // Card number: 13–19 digits, optionally grouped by spaces/hyphens. Keep first 4 + last 4.
        Rule(
            type = PiiType.CARD_NUMBER,
            regex = Regex("""\b(?:\d[ -]?){12,18}\d\b"""),
            mask = { m -> maskKeepEnds(m.value, first = 4, last = 4) },
        ),
        // Aadhaar: exactly 12 digits (optionally grouped 4-4-4). Keep last 4.
        Rule(
            type = PiiType.AADHAAR,
            regex = Regex("""\b\d{4}[ -]?\d{4}[ -]?\d{4}\b"""),
            mask = { m -> maskKeepLast(m.value, keep = 4) },
        ),
        // Bank account: 9–18 digit run. Keep last 4. (Runs after card/Aadhaar so those win first.)
        Rule(
            type = PiiType.BANK_ACCOUNT,
            regex = Regex("""\b\d{9,18}\b"""),
            mask = { m -> maskKeepLast(m.value, keep = 4) },
        ),
        // Bare OTP fallback: a standalone 6-digit code not already masked (e.g. "123456").
        Rule(
            type = PiiType.OTP,
            regex = Regex("""\b\d{6}\b"""),
            mask = { m -> maskAll(m.value) },
        ),
    )
}
