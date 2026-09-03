package com.safecheck.android.data.api

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
 * Real HTTP implementation of [SafeCheckApi] (Phase 8). Same interface as the mock, so it is
 * swapped in [com.safecheck.android.di.AppContainer] by the USE_MOCK_API flag with no change
 * to domain or UI (requirements R-10.1.3). Uses the exact contract in Master Spec §27.
 */
class RetrofitSafeCheckApi(baseUrl: String) : SafeCheckApi {

    private val service: SafeCheckService =
        HttpClient.retrofit(baseUrl).create(SafeCheckService::class.java)

    override suspend fun check(request: CheckRequest): CheckResponse = service.check(request)
    override suspend fun document(request: DocumentRequest): DocumentResponse = service.document(request)
    override suspend fun shareToCircle(request: ShareRequest): ShareResponse = service.shareToCircle(request)
    override suspend fun submitReview(request: ReviewRequest): ReviewResponse = service.submitReview(request)
    override suspend fun recordIncident(request: IncidentRequest): IncidentResponse = service.recordIncident(request)
    override suspend fun history(): List<SafetyCaseSummary> = service.history()
}
