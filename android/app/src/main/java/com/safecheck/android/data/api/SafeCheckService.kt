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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Retrofit mapping of the shared SafeCheck API (Master Spec §27). */
interface SafeCheckService {
    @POST("v1/check")
    suspend fun check(@Body request: CheckRequest): CheckResponse

    @POST("v1/document")
    suspend fun document(@Body request: DocumentRequest): DocumentResponse

    @POST("v1/safety-circle/share")
    suspend fun shareToCircle(@Body request: ShareRequest): ShareResponse

    @POST("v1/safety-circle/review")
    suspend fun submitReview(@Body request: ReviewRequest): ReviewResponse

    @POST("v1/recovery/incident")
    suspend fun recordIncident(@Body request: IncidentRequest): IncidentResponse

    @GET("v1/history")
    suspend fun history(): List<SafetyCaseSummary>
}
