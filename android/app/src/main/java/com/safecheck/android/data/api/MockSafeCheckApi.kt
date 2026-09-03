package com.safecheck.android.data.api

import com.safecheck.android.data.api.dto.CheckRequest
import com.safecheck.android.data.api.dto.CheckResponse
import com.safecheck.android.data.api.dto.DocumentRequest
import com.safecheck.android.data.api.dto.DocumentResponse
import com.safecheck.android.data.api.dto.EvidenceDto
import com.safecheck.android.data.api.dto.IncidentRequest
import com.safecheck.android.data.api.dto.IncidentResponse
import com.safecheck.android.data.api.dto.ModelVersionsDto
import com.safecheck.android.data.api.dto.ReviewRequest
import com.safecheck.android.data.api.dto.ReviewResponse
import com.safecheck.android.data.api.dto.SafetyCaseSummary
import com.safecheck.android.data.api.dto.ShareRequest
import com.safecheck.android.data.api.dto.ShareResponse
import com.safecheck.android.data.api.dto.SubScoresDto
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Deterministic, spec-accurate stand-in for the shared Safety Brain (design.md §4).
 * Used while BuildConfig.USE_MOCK_API == true so the whole app is functional before the
 * backend exists. It mimics the deterministic risk engine's OUTPUT for demo scenarios; it
 * does not re-implement the engine's internals, and evidence always sums to the score
 * (requirements R-6.2.2, R-10.1, R-11).
 *
 * This mock never fabricates external reputation for real services; when it wants to
 * demonstrate degraded behavior it lists the missing check in `unavailable_signals`
 * (requirements R-10.2, Master Spec §15.7).
 */
class MockSafeCheckApi : SafeCheckApi {

    override suspend fun check(request: CheckRequest): CheckResponse {
        delay(350) // simulate processing latency so Analyzing state is cleanly visible
        return com.safecheck.android.domain.analysis.DeterministicRiskEngine.evaluate(request)
    }

    override suspend fun document(request: DocumentRequest): DocumentResponse {
        delay(400)
        return com.safecheck.android.domain.analysis.DeterministicRiskEngine.evaluateDocument(request)
    }

    override suspend fun shareToCircle(request: ShareRequest): ShareResponse {
        delay(250)
        return ShareResponse(
            reviewLink = "https://safecheck.local/review/${UUID.randomUUID()}",
            expiresInMinutes = 120, // 2-hour expiring link (Master Spec §21)
        )
    }

    override suspend fun submitReview(request: ReviewRequest): ReviewResponse {
        delay(250)
        return ReviewResponse(request.caseId, request.decision, request.note)
    }

    override suspend fun recordIncident(request: IncidentRequest): IncidentResponse {
        delay(200)
        return IncidentResponse(request.caseId, request.incidentState, stored = true)
    }

    override suspend fun history(): List<SafetyCaseSummary> = emptyList()
}
