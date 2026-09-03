package com.safecheck.android.domain.analysis

import com.google.common.net.InternetDomainName
import java.net.URI

data class UrlEvidence(
    val label: String,
    val points: Int,
    val observedValue: String,
)

data class UrlAnalysisResult(
    val urls: List<String>,
    val totalUrlPts: Int,
    val evidence: List<UrlEvidence>,
    val primaryDomain: String?,
    val isLookalike: Boolean,
)

/**
 * Hardened domain intelligence engine (P1/P2 architecture).
 * Uses Public Suffix List parsing, Levenshtein edit distance, punycode detection,
 * URL shortener classification, and structure validation to detect lookalikes and phishing infrastructure.
 */
object DomainIntelligenceEngine {

    private val URL_REGEX = Regex("(?i)\\b(?:https?://|www\\.)[a-zA-Z0-9.\\-_]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?\\b")
    private val NAKED_DOMAIN_REGEX = Regex("(?i)\\b[a-zA-Z0-9][a-zA-Z0-9.\\-_]*\\.(?:com|org|net|xyz|top|in|co\\.in|gov\\.in|edu|info|biz|club|site|online|link|click|zip|live|store|shop|me|io|ai|app|dev|tech|pro|tv|cc|ly|ru|su|cfd|work|vip|fit|tokyo|kim|icu|buzz)(?::[0-9]+)?(?:/[^\\s]*)?\\b")
    private val IP_REGEX = Regex("(?i)\\b(?:https?://)?([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(?::[0-9]+)?(?:/[^\\s]*)?\\b")

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "click", "link", "zip", "ru", "surf", "work", "rest", "country", "vip", "fit", "tokyo", "kim", "su", "cfd"
    )

    private val URL_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "cutt.ly", "rb.gy", "shorturl.at", "ow.ly", "rebrand.ly"
    )

    // Brand target mapping: brand keyword -> official domain
    private val BRAND_TARGETS = mapOf(
        "sbi" to "sbi.co.in",
        "onlinesbi" to "onlinesbi.sbi",
        "yono" to "onlinesbi.sbi",
        "hdfc" to "hdfcbank.com",
        "hdfcbank" to "hdfcbank.com",
        "icici" to "icicibank.com",
        "icicibank" to "icicibank.com",
        "axis" to "axisbank.com",
        "axisbank" to "axisbank.com",
        "pnb" to "pnbindia.in",
        "pnbindia" to "pnbindia.in",
        "incometax" to "incometax.gov.in",
        "cybercrime" to "cybercrime.gov.in",
        "indiapost" to "indiapost.gov.in",
        "amazon" to "amazon.in",
        "flipkart" to "flipkart.com",
        "paytm" to "paytm.com",
        "uidai" to "uidai.gov.in",
    )

    fun analyze(text: String): UrlAnalysisResult {
        val trimmed = text.trim()
        val directUrl = if (!trimmed.contains(" ") && trimmed.contains(".") && trimmed.length >= 4) {
            listOf(trimmed)
        } else {
            emptyList()
        }

        val urlsFound = (URL_REGEX.findAll(text).map { it.value } +
                NAKED_DOMAIN_REGEX.findAll(text).map { it.value } +
                IP_REGEX.findAll(text).map { it.value } +
                directUrl)
            .distinct()
            .toList()

        if (urlsFound.isEmpty()) {
            return UrlAnalysisResult(
                urls = emptyList(),
                totalUrlPts = 0,
                evidence = emptyList(),
                primaryDomain = null,
                isLookalike = false,
            )
        }

        val evidenceList = mutableListOf<UrlEvidence>()
        var primaryDomain: String? = null
        var lookalikeDetected = false

        for (urlStr in urlsFound) {
            val normalizedUrl = if (!urlStr.startsWith("http://", ignoreCase = true) && !urlStr.startsWith("https://", ignoreCase = true)) {
                "https://$urlStr"
            } else {
                urlStr
            }

            val uri = runCatching { URI(normalizedUrl) }.getOrNull()
            val host = uri?.host?.lowercase() ?: run {
                urlStr.substringBefore("/").substringAfter("://").substringBefore(":").lowercase()
            }

            if (primaryDomain == null && host.isNotBlank()) {
                primaryDomain = host
            }

            // 1. Raw IPv4 / IPv6 host check
            val isIp = IP_REGEX.matches(urlStr) || host.matches(Regex("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$"))
            if (isIp) {
                evidenceList.add(
                    UrlEvidence(
                        label = "Raw IP Hostname",
                        points = 20,
                        observedValue = host,
                    )
                )
            }

            // 2. User-info '@' trick (e.g. http://sbi.co.in@attacker.com)
            if (urlStr.contains("@") && uri?.userInfo != null) {
                evidenceList.add(
                    UrlEvidence(
                        label = "Deceptive User-Info '@' in URL",
                        points = 20,
                        observedValue = urlStr.take(35),
                    )
                )
            }

            // 3. Insecure HTTP Scheme
            if (urlStr.startsWith("http://", ignoreCase = true) && !host.startsWith("localhost") && !host.startsWith("127.0.0.1")) {
                evidenceList.add(
                    UrlEvidence(
                        label = "Unencrypted HTTP Scheme",
                        points = 8,
                        observedValue = urlStr.take(30),
                    )
                )
            }

            if (!isIp && host.contains(".")) {
                // Public Suffix List (PSL) parsing via Guava
                val topPrivateDomain = runCatching {
                    val idn = InternetDomainName.from(host)
                    if (idn.isUnderPublicSuffix) idn.topPrivateDomain().toString() else host
                }.getOrDefault(host)

                val publicSuffix = runCatching {
                    val idn = InternetDomainName.from(host)
                    if (idn.isUnderPublicSuffix) idn.publicSuffix().toString() else host.substringAfterLast('.')
                }.getOrDefault(host.substringAfterLast('.'))

                // Second-level domain (SLD) without the public suffix
                val sld = topPrivateDomain.removeSuffix(".$publicSuffix").lowercase()

                // 4. Suspicious / Abused TLD
                if (SUSPICIOUS_TLDS.contains(publicSuffix.lowercase())) {
                    evidenceList.add(
                        UrlEvidence(
                            label = "High-Risk TLD (.$publicSuffix)",
                            points = 10,
                            observedValue = topPrivateDomain,
                        )
                    )
                }

                // 5. URL Shortener Detection
                if (URL_SHORTENERS.contains(topPrivateDomain.lowercase())) {
                    evidenceList.add(
                        UrlEvidence(
                            label = "URL Shortener Masking",
                            points = 8,
                            observedValue = topPrivateDomain,
                        )
                    )
                }

                // 6. Punycode / Internationalized Domain Name (IDN) homoglyph
                if (host.contains("xn--", ignoreCase = true)) {
                    evidenceList.add(
                        UrlEvidence(
                            label = "Punycode Homoglyph Deception",
                            points = 15,
                            observedValue = host,
                        )
                    )
                }

                // 7. Lookalike & Brand Impersonation Check (SLD analysis)
                val lookalikeMatch = evaluateLookalike(host, topPrivateDomain, sld)
                if (lookalikeMatch != null) {
                    lookalikeDetected = true
                    evidenceList.add(
                        UrlEvidence(
                            label = "Lookalike Domain",
                            points = 25,
                            observedValue = "$topPrivateDomain (imitates $lookalikeMatch)",
                        )
                    )
                }
            }
        }

        // Correlation capping: Lookalike domain threat already encompasses syntax/TLD hazards on that deceptive host
        val cappedPts = if (lookalikeDetected) {
            25
        } else {
            evidenceList.sumOf { it.points }.coerceAtMost(30)
        }

        // Adjust itemized points so they sum exactly to cappedPts
        val normalizedEvidence = normalizePoints(evidenceList, cappedPts)

        return UrlAnalysisResult(
            urls = urlsFound,
            totalUrlPts = cappedPts,
            evidence = normalizedEvidence,
            primaryDomain = primaryDomain,
            isLookalike = lookalikeDetected,
        )
    }

    private fun evaluateLookalike(fullHost: String, topPrivateDomain: String, sld: String): String? {
        // If the domain or host is an official domain for ANY brand target, it is legitimate!
        if (BRAND_TARGETS.values.any { topPrivateDomain == it || fullHost == it || fullHost.endsWith(".$it") || fullHost.endsWith(".sbi") }) {
            return null
        }

        for ((brandKey, officialDomain) in BRAND_TARGETS) {
            // If it IS the official domain or a valid subdomain of it, it is NOT a lookalike
            if (topPrivateDomain == officialDomain || fullHost.endsWith(".$officialDomain")) {
                continue
            }

            // A) Exact brand key with deceptive separators (e.g. sbi-kyc-update, hdfc-login, icici-verify)
            val isBrandWithDeceptiveSeparators = sld.startsWith("$brandKey-") ||
                sld.endsWith("-$brandKey") ||
                sld.contains("-$brandKey-") ||
                sld == brandKey

            // B) Deceptive subdomain: brand name appears as subdomain on an unrelated domain (e.g. sbi.co.in.attacker.xyz)
            val isDeceptiveSubdomain = fullHost.contains("$brandKey.") && !fullHost.endsWith(".$officialDomain")

            // C) Levenshtein distance 1 or 2 on words of similar length (e.g. sbikyc -> sbi, iciccbk -> icicibank)
            val distance = levenshtein(sld, brandKey)
            val isTypoDistance = distance in 1..2 && (sld.length in (brandKey.length - 2)..(brandKey.length + 3))

            if (isBrandWithDeceptiveSeparators || isDeceptiveSubdomain || isTypoDistance) {
                return officialDomain
            }
        }
        return null
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[a.length][b.length]
    }

    private fun normalizePoints(evidence: List<UrlEvidence>, targetTotal: Int): List<UrlEvidence> {
        if (evidence.isEmpty() || targetTotal == 0) return emptyList()
        val rawSum = evidence.sumOf { it.points }
        if (rawSum == targetTotal) return evidence

        val scaled = evidence.map { ev ->
            val pts = ((ev.points.toDouble() / rawSum) * targetTotal).toInt().coerceAtLeast(1)
            ev.copy(points = pts)
        }.toMutableList()

        val diff = targetTotal - scaled.sumOf { it.points }
        if (scaled.isNotEmpty() && diff != 0) {
            val last = scaled.last()
            scaled[scaled.lastIndex] = last.copy(points = (last.points + diff).coerceAtLeast(1))
        }
        return scaled
    }
}
