package com.safecheck.android.di

import android.content.Context
import androidx.room.Room
import com.safecheck.android.BuildConfig
import com.safecheck.android.data.api.MockSafeCheckApi
import com.safecheck.android.data.api.ResilientSafeCheckApi
import com.safecheck.android.data.api.RetrofitSafeCheckApi
import com.safecheck.android.data.api.SafeCheckApi
import com.safecheck.android.data.store.CaseStore
import com.safecheck.android.data.store.ContactStore
import com.safecheck.android.data.store.RoomCaseStore
import com.safecheck.android.data.store.db.SafeCheckDatabase
import com.safecheck.android.domain.usecase.RecordRecoveryIncidentUseCase
import com.safecheck.android.domain.usecase.ShareToSafetyCircleUseCase
import com.safecheck.android.data.store.SettingsStore
import com.safecheck.android.notify.RiskNotifier
import com.safecheck.android.sms.DemoSmsTrigger
import com.safecheck.android.sms.SmsIngestion
import com.safecheck.android.data.extract.OcrExtractor
import com.safecheck.android.data.extract.PdfTextExtractor
import com.safecheck.android.domain.redaction.RedactionEngine
import com.safecheck.android.domain.usecase.AnalyzeContentUseCase
import com.safecheck.android.domain.usecase.SubmitDocumentUseCase

/**
 * Manual dependency container / composition root (design.md §1 — no Hilt).
 * The single place where mock vs real API is chosen (BuildConfig.USE_MOCK_API).
 */
class AppContainer(private val appContext: Context) {

    val context: Context get() = appContext

    private val database: SafeCheckDatabase by lazy {
        Room.databaseBuilder(appContext, SafeCheckDatabase::class.java, "safecheck.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val redactionEngine: RedactionEngine by lazy { RedactionEngine() }

    val ocrExtractor: OcrExtractor by lazy { OcrExtractor(appContext) }

    val pdfTextExtractor: PdfTextExtractor by lazy { PdfTextExtractor(appContext) }

    /**
     * Phase 3 uses the deterministic mock. Phase 8 adds RetrofitSafeCheckApi and selects it
     * when BuildConfig.USE_MOCK_API == false — with no change to domain or UI.
     */
    val safeCheckApi: SafeCheckApi by lazy {
        if (BuildConfig.USE_MOCK_API) {
            MockSafeCheckApi()
        } else {
            // Real shared API. Wrapped so transport failures degrade gracefully to controlled
            // demo data instead of crashing the journey (requirements R-10.2).
            ResilientSafeCheckApi(
                primary = RetrofitSafeCheckApi(BuildConfig.API_BASE_URL),
                fallback = MockSafeCheckApi(),
            )
        }
    }

    val caseStore: CaseStore by lazy { RoomCaseStore(database.caseDao()) }

    val analyzeContentUseCase: AnalyzeContentUseCase by lazy {
        AnalyzeContentUseCase(redactionEngine, safeCheckApi, caseStore)
    }

    val submitDocumentUseCase: SubmitDocumentUseCase by lazy {
        SubmitDocumentUseCase(appContext, pdfTextExtractor, redactionEngine, safeCheckApi, caseStore)
    }

    val contactStore: ContactStore by lazy { ContactStore(database.contactDao()) }

    val shareToSafetyCircleUseCase: ShareToSafetyCircleUseCase by lazy {
        ShareToSafetyCircleUseCase(safeCheckApi)
    }

    val recordRecoveryIncidentUseCase: RecordRecoveryIncidentUseCase by lazy {
        RecordRecoveryIncidentUseCase(safeCheckApi)
    }

    val riskNotifier: RiskNotifier by lazy { RiskNotifier(appContext) }

    /**
     * The single SMS pipeline shared by real and demo sources (design.md §5). On case-ready
     * it posts the local risk notification; opening it lands on the same Risk Result.
     */
    val smsIngestion: SmsIngestion by lazy {
        SmsIngestion(
            analyze = analyzeContentUseCase,
            onCaseReady = { case -> riskNotifier.notifyCase(case) },
        )
    }

    val demoSmsTrigger: DemoSmsTrigger by lazy { DemoSmsTrigger(smsIngestion) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext, database.auditDao()) }
}
