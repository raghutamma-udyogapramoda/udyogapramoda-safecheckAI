package com.safecheck.android.domain.usecase

import com.safecheck.android.data.api.SafeCheckApi
import com.safecheck.android.data.api.dto.IncidentRequest

/**
 * Records a Recovery incident step to the shared API (R-9). Enforces the zero
 * OTP/PIN/password rule: only the stage name, the completed action labels, and an optional
 * outcome are sent — never secrets or raw content (Master Spec §22, §28).
 */
class RecordRecoveryIncidentUseCase(
    private val api: SafeCheckApi,
) {
    suspend operator fun invoke(
        caseId: String,
        incidentState: String,
        recoveryActions: List<String>,
        outcome: String? = null,
    ): Boolean {
        val response = api.recordIncident(
            IncidentRequest(
                caseId = caseId,
                incidentState = incidentState,
                recoveryActions = recoveryActions,
                outcome = outcome,
            )
        )
        return response.stored
    }
}
