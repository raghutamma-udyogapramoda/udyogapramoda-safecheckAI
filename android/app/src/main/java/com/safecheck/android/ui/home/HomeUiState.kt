package com.safecheck.android.ui.home

import com.safecheck.android.ui.theme.RiskBand

/** Lightweight UI item for the Home recent-activity list. */
data class RecentCaseItem(
    val caseId: String,
    val title: String,
    val subtitle: String,
    val band: RiskBand,
    val score: Int,
)

/** Home screen state (requirements R-2.1). */
data class HomeUiState(
    val protectionSummary: String = "Manual protection active",
    val recent: List<RecentCaseItem> = emptyList(),
)
