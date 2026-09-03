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
 * The SINGLE boundary between the Android client and the shared Safety Brain
 * (Master Spec §27, design.md §4). Two implementations sit behind this interface:
 *
 *  - [MockSafeCheckApi] — deterministic, spec-accurate; used while USE_MOCK_API == true.
 *  - RetrofitSafeCheckApi — real HTTP (Phase 8); same interface, swapped in AppContainer.
 *
 * The client NEVER computes the risk verdict; it only submits (already-redacted) input and
 * renders the response (requirements R-1.2, R-10.1).
 */
interface SafeCheckApi {
    suspend fun check(request: CheckRequest): CheckResponse
    suspend fun document(request: DocumentRequest): DocumentResponse
    suspend fun shareToCircle(request: ShareRequest): ShareResponse
    suspend fun submitReview(request: ReviewRequest): ReviewResponse
    suspend fun recordIncident(request: IncidentRequest): IncidentResponse
    suspend fun history(): List<SafetyCaseSummary>
}
