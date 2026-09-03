package com.safecheck.android.data.api

import android.util.Log
import com.safecheck.android.data.api.dto.CheckRequest
import com.safecheck.android.data.api.dto.CheckResponse
import com.safecheck.android.data.api.dto.DocumentRequest
import com.safecheck.android.data.api.dto.DocumentResponse
import com.safecheck.android.data.api.dto.IncidentRequest
import com.safecheck.android.data.api.dto.IncidentResponse
import com.safecheck.android.data.api.dto.ReviewRequest
import com.safecheck.android.data.api.dto.ReviewResponse
import com.safecheck.android.data.api.dto.SafetyCaseSummary
import com.safecheck.android.data.api.dto.ShareRequest
import com.safecheck.android.data.api.dto.ShareResponse

/**
 * Wraps the real API so that transport-level failures (network down, backend unavailable,
 * timeout) keep the journey functional (requirements R-10.2, Master Spec §15.7). On failure
 * it falls back to the deterministic mock — controlled demo data, NOT fabricated external
 * reputation. The mock already surfaces `unavailable_signals` honestly, so the UI shows what
 * could not be reached rather than implying full confidence.
 */
class ResilientSafeCheckApi(
    private val primary: SafeCheckApi,
    private val fallback: SafeCheckApi,
) : SafeCheckApi {

    override suspend fun check(request: CheckRequest): CheckResponse =
        attempt("check") { primary.check(request) } ?: fallback.check(request)

    override suspend fun document(request: DocumentRequest): DocumentResponse =
        attempt("document") { primary.document(request) } ?: fallback.document(request)

    override suspend fun shareToCircle(request: ShareRequest): ShareResponse =
        attempt("share") { primary.shareToCircle(request) } ?: fallback.shareToCircle(request)

    override suspend fun submitReview(request: ReviewRequest): ReviewResponse =
        attempt("review") { primary.submitReview(request) } ?: fallback.submitReview(request)

    override suspend fun recordIncident(request: IncidentRequest): IncidentResponse =
        attempt("incident") { primary.recordIncident(request) } ?: fallback.recordIncident(request)

    override suspend fun history(): List<SafetyCaseSummary> =
        attempt("history") { primary.history() } ?: emptyList()

    private inline fun <T> attempt(op: String, block: () -> T): T? = try {
        block()
    } catch (t: Throwable) {
        Log.w("SafeCheck", "API '$op' failed, using fallback: ${t.message}")
        null
    }
}
