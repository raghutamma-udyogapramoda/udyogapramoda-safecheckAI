# SafeCheck Android — Implementation Tasks

**Developer:** Developer 1 (Android client only)
**Companions:** `requirements.md`, `design.md` (both approved)
**Timeline:** ~2.5 days, one developer
**Status:** Draft for review → then sequential execution

---

## How to use this file

- Phases run in the **mandatory order Phase 0 → Phase 12** (instruction #25).
- Execute tasks **sequentially**. For each: implement → build/test → verify acceptance → fix → continue (instruction #29).
- **Do not start a P1 task until all P0 is stable.** Do not pull P1/P2 features into P0.
- Every task lists: **Purpose · Priority · Files · Dependencies · Acceptance · Complexity**.
- Complexity scale: **S** (≲30 min), **M** (≲2 h), **L** (≳2 h).
- Requirement/design references in parentheses map back to the approved docs.

**Critical spine (build in this order, do not reorder):**
Project builds → design system + nav shell → Home + Manual Check UI →
**first complete vertical slice** (manual input → redaction → mock API → Risk Result → evidence → explanation → safe action) →
screenshot/URL/QR → document → Safety Circle → Recovery → real API swap →
**real SMS (same SmsIngestion pipeline)** → notifications → demo simulation + scenarios → test/polish.

---

## PHASE 0 — Project setup + build verification

### T0.1 — Create Android project skeleton
- **Purpose:** A compiling single-module Android app to build on.
- **Priority:** P0
- **Files:** `/android` (Gradle project), `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `SafeCheckApp.kt` (Application)
- **Dependencies:** none
- **Acceptance:** `./gradlew assembleDebug` succeeds; app launches to a blank Compose screen; min SDK 26 / target 34 (design §1).
- **Complexity:** M

### T0.2 — Add dependencies & config flag scaffold
- **Purpose:** Wire the libraries the design chose, plus the mock/real API flag.
- **Priority:** P0
- **Files:** `app/build.gradle.kts` (Compose, Nav-Compose, coroutines, Retrofit, OkHttp, kotlinx.serialization, Room, DataStore, CameraX, ML Kit barcode + text-recognition), `BuildConfig` flag `USE_MOCK_API=true`
- **Dependencies:** T0.1
- **Acceptance:** Project syncs and builds with all deps; `BuildConfig.USE_MOCK_API` readable at runtime.
- **Complexity:** M

### T0.3 — AppContainer (manual DI)
- **Purpose:** One place to construct/swap dependencies (design §1, §4).
- **Priority:** P0
- **Files:** `di/AppContainer.kt`
- **Dependencies:** T0.2
- **Acceptance:** `AppContainer` instantiated in `SafeCheckApp`; exposes lazy singletons (api, stores, engines) — stubs allowed for now; builds.
- **Complexity:** S

---

## PHASE 1 — Design system + navigation + app shell

### T1.1 — SafeCheck theme & design tokens
- **Purpose:** Brand identity + risk-band color tokens (R-1.3, design §8).
- **Priority:** P0
- **Files:** `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `RiskTokens.kt` (LOW=green, MEDIUM=amber, HIGH=red)
- **Dependencies:** T0.1
- **Acceptance:** Theme applies; risk color tokens resolve; large-text font-scale hook present.
- **Complexity:** M

### T1.2 — Reusable components
- **Purpose:** Shared UI vocabulary (design §8).
- **Priority:** P0
- **Files:** `ui/components/RiskCard.kt`, `EvidenceRow.kt`, `SafeCheckButton.kt`, `ChannelToggle.kt`, `StageStepper.kt`, `StatusBanner.kt`
- **Dependencies:** T1.1
- **Acceptance:** Each renders in a Compose preview with sample data; terminology matches spec.
- **Complexity:** M

### T1.3 — Navigation shell (bottom nav + routes)
- **Purpose:** Single-Activity mobile navigation (design §8).
- **Priority:** P0
- **Files:** `ui/nav/SafeCheckNavHost.kt`, `ui/nav/Destinations.kt`
- **Dependencies:** T1.2
- **Acceptance:** Bottom nav (Home · Protection · Circle · Recovery) switches between placeholder screens; detail routes (ManualCheck, RiskResult, Privacy) navigable.
- **Complexity:** M

---

## PHASE 2 — Home + Manual Check (UI)

### T2.1 — Home screen
- **Purpose:** Protection status + entry points + recent activity (R-2.1).
- **Priority:** P0
- **Files:** `ui/home/HomeScreen.kt`, `HomeViewModel.kt`
- **Dependencies:** T1.3
- **Acceptance:** Shows branding, status summary, entry points (Manual Check, Automatic Protection, Safety Circle, Recovery, Privacy), recent-activity list with empty state.
- **Complexity:** M

### T2.2 — Manual Check input UI
- **Purpose:** Tabbed input capture (R-3.1, R-3.2, R-3.5; camera/OCR/PDF wired later).
- **Priority:** P0
- **Files:** `ui/manual/ManualCheckScreen.kt`, `ManualCheckViewModel.kt`
- **Dependencies:** T2.1
- **Acceptance:** Tabs Text · URL · QR · Screenshot · Email · Document render; Text/URL/Email accept input and expose a submit action (no analysis wired yet); idle/analyzing/error states stubbed.
- **Complexity:** M

---

## PHASE 3 — First complete vertical slice (the priority)

> Goal: **Manual input → redaction → mock API → Risk Result → evidence → explanation → safe action**, fully working end-to-end for text/URL/email.

### T3.1 — Domain models
- **Purpose:** Case/evidence/result types aligned to spec §28 (design §7).
- **Priority:** P0
- **Files:** `domain/model/SafetyCase.kt`, `Evidence.kt`, `RiskResult.kt`, `SubScores.kt`, `Incident.kt`, `Review.kt`, `TrustedContact.kt`, `AuditLogEntry.kt`
- **Dependencies:** T0.1
- **Acceptance:** Models compile; `SafetyCase` includes `source_type`, `sub_scores`, `evidence`, `unavailable_signals`, `explanation`, `recommended_actions` (design §4, §7).
- **Complexity:** M

### T3.2 — RedactionEngine + unit tests
- **Purpose:** The single on-device privacy choke-point (R-8.1, design §3).
- **Priority:** P0
- **Files:** `domain/redaction/RedactionEngine.kt`, `RedactionHit.kt`, `test/RedactionEngineTest.kt`
- **Dependencies:** T3.1
- **Acceptance:** Masks OTP/card/account/IFSC/UPI/PAN/Aadhaar/password/PIN; passes exact spec examples (`123456→******`, `4111 1111 1111 1111→4111 **** **** 1111`, `123456789012→********9012`); `RedactionHit` stores type only. Unit tests green.
- **Complexity:** L

### T3.3 — SafeCheckApi interface + DTOs
- **Purpose:** The single shared-brain boundary (R-10.1, design §4).
- **Priority:** P0
- **Files:** `data/api/SafeCheckApi.kt`, `data/api/dto/*.kt` (CheckRequest/Response, Document, Share, Review, Incident, SafetyCaseSummary)
- **Dependencies:** T3.1
- **Acceptance:** Interface matches spec §27 endpoints; DTOs carry `input_type`, `source_type`, `redaction_hits` (types), `risk_score`, `risk_level`, `evidence[]`, `sub_scores`, `explanation`, `recommended_actions`, `unavailable_signals`.
- **Complexity:** M

### T3.4 — MockSafeCheckApi (deterministic)
- **Purpose:** Spec-accurate results so the app works before backend exists (R-10.1, R-11, design §4).
- **Priority:** P0
- **Files:** `data/api/MockSafeCheckApi.kt`
- **Dependencies:** T3.3
- **Acceptance:** Returns HIGH case `ml_pts=42 + url_pts=25 + rule_pts=20 = 87/100` with evidence items summing exactly to 87 (False Urgency +10, Unauthorized Payment Ask +10, Lookalike Domain 25 URL pts, ML contribution); returns a clearly-LOW safe case; can emit `unavailable_signals`. Selected when `USE_MOCK_API=true`.
- **Complexity:** L

### T3.5 — AnalyzeContentUseCase + wiring
- **Purpose:** Connect input → redaction → api → result (R-1.2 no local scoring).
- **Priority:** P0
- **Files:** `domain/usecase/AnalyzeContentUseCase.kt`; update `ManualCheckViewModel`, `AppContainer`
- **Dependencies:** T3.2, T3.4
- **Acceptance:** Submitting text/URL/email runs redaction then `api.check(...)`; result mapped to `SafetyCase`; score/level come solely from the response.
- **Complexity:** M

### T3.6 — Risk Result screen (Card + Evidence + Explanation + Safe Actions)
- **Purpose:** Render the structured result (R-6.1–R-6.4, design §8).
- **Priority:** P0
- **Files:** `ui/result/RiskResultScreen.kt`, `RiskResultViewModel.kt`
- **Dependencies:** T3.5
- **Acceptance:** Shows band + `X/100` + "Score Immutable by LLM"; Evidence rows with points and sub-engine subtotals that **sum exactly** to the score; plain-language explanation; recommended actions; Safety Circle + Recovery entry buttons; signal-unavailable chip when present.
- **Complexity:** L

### T3.7 — CaseStore + local persistence (minimal retention)
- **Purpose:** Persist sanitized cases; feed Home recent activity (R-8.2).
- **Priority:** P0
- **Files:** `data/store/CaseStore.kt`, Room entities/DAO
- **Dependencies:** T3.6
- **Acceptance:** Sanitized `SafetyCase` saved and listed on Home; raw content not persisted; opening a case reopens Risk Result.
- **Complexity:** M

> **Milestone 1:** Vertical slice demoable for text/URL/email (Scenarios 2 low + high via mock).

---

## PHASE 4 — Screenshot / URL / QR extraction

### T4.1 — QR scanning (camera)
- **Purpose:** On-device QR decode before opening (R-3.3, design §6).
- **Priority:** P0
- **Files:** `data/extract/QrScanner.kt`, `ui/manual/QrScanScreen.kt`; camera permission handling
- **Dependencies:** T3.5
- **Acceptance:** Camera decodes QR to payload on-device → same redaction + `check(input_type="qr")` path; target never auto-opened; permission-denied → manual paste fallback.
- **Complexity:** L

### T4.2 — Screenshot/image OCR
- **Purpose:** Extract text from an image (R-3.4, design §6).
- **Priority:** P0
- **Files:** `data/extract/OcrExtractor.kt`; image picker in Manual Check
- **Dependencies:** T3.5
- **Acceptance:** Selected image → OCR text → redaction → `check(input_type="screenshot")`; OCR failure shows honest error + manual paste fallback.
- **Complexity:** L

### T4.3 — URL/QR result polish
- **Purpose:** Ensure URL/domain intelligence evidence renders (R-11.1.3).
- **Priority:** P0
- **Files:** update `RiskResultScreen`, `MockSafeCheckApi` (URL/QR sample)
- **Dependencies:** T4.1
- **Acceptance:** Suspicious URL/QR returns evidence with URL points and lookalike-domain indicator; safe action shown.
- **Complexity:** S

---

## PHASE 5 — Document / PDF core flow

### T5.1 — PDF text extraction + sample fallback
- **Purpose:** Smallest reliable document journey (R-3.6, design §6).
- **Priority:** P0
- **Files:** `data/extract/PdfTextExtractor.kt`, bundled `assets/sample_document.pdf`
- **Dependencies:** T3.5
- **Acceptance:** PDF picked → text extracted → `document(...)`; if extraction too sparse, bundled sample keeps journey working.
- **Complexity:** L

### T5.2 — Document result rendering
- **Purpose:** Surface document intelligence output (R-3.6.2, R-11.1.4).
- **Priority:** P0
- **Files:** `ui/result/DocumentResultScreen.kt` (or extend RiskResult), `MockSafeCheckApi` document response
- **Dependencies:** T5.1
- **Acceptance:** Shows key info, deadlines, required actions, suspicious URL/payment/contact, simplified explanation, and risk where applicable.
- **Complexity:** M

---

## PHASE 6 — Safety Circle

### T6.1 — Trusted contacts (local/mock)
- **Purpose:** Contacts to share with (R-7.1.1, design §10).
- **Priority:** P0
- **Files:** `ui/circle/SafetyCircleScreen.kt`, `SafetyCircleViewModel.kt`, `data/store/ContactStore.kt`
- **Dependencies:** T3.7
- **Acceptance:** Add/list local trusted contacts; empty state handled.
- **Complexity:** M

### T6.2 — Share sanitized case + advisory view
- **Purpose:** Advisory second opinion, sanitized (R-7.1.2–R-7.1.5).
- **Priority:** P0
- **Files:** update SafetyCircle screens; `ShareToSafetyCircleUseCase`, `SubmitReviewUseCase`; `MockSafeCheckApi` share/review
- **Dependencies:** T6.1, T3.6
- **Acceptance:** Share builds a sanitized summary (band/score/evidence labels only — no raw values/secrets); advisory opinion (simulated, labeled) shows beside the immutable score; no-response defaults to safe recommendation; contact never overrides decision.
- **Complexity:** L

---

## PHASE 7 — Recovery

### T7.1 — Recovery wizard (5 stages)
- **Purpose:** Guided STOP→SECURE→REPORT→DOCUMENT→LEARN/PREVENT (R-9.1, design §11).
- **Priority:** P0
- **Files:** `ui/recovery/RecoveryScreen.kt`, `RecoveryViewModel.kt`, `RecordRecoveryIncidentUseCase`
- **Dependencies:** T3.6 (entry from result), T2.1 (entry from Home)
- **Acceptance:** Reachable from any Risk Result and from Home; StageStepper with immediate actions, warnings, progress, completion; REPORT surfaces 1930 / cybercrime.gov.in / RBI 14440; DOCUMENT builds an incident summary via `recordIncident(...)` with zero OTP/PIN/password.
- **Complexity:** L

> **Milestone 2:** Full manual product journey demoable (Scenarios 1–5 via mock, incl. Safety Circle + Recovery).

---

## PHASE 8 — Real backend/API integration

### T8.1 — RetrofitSafeCheckApi + swap
- **Purpose:** Same interface, real HTTP, one-flag swap (R-10.1.3, design §4).
- **Priority:** P0
- **Files:** `data/api/RetrofitSafeCheckApi.kt`, `data/api/HttpClient.kt`; `AppContainer` selects by `USE_MOCK_API`
- **Dependencies:** T3.3
- **Acceptance:** With `USE_MOCK_API=false`, calls hit the configured base URL; no domain/UI change; mock remains default until backend is confirmed.
- **Complexity:** M

### T8.2 — Graceful degradation
- **Purpose:** Keep journey alive on failures; honest signals (R-10.2, design §4).
- **Priority:** P0
- **Files:** `data/api/*` error handling; `StatusBanner` usage
- **Dependencies:** T8.1
- **Acceptance:** Network/backend/OCR/camera/permission failures never crash; `unavailable_signals` rendered honestly; total-failure falls back to controlled demo data; no fabricated reputation.
- **Complexity:** M

---

## PHASE 9 — Real SMS automatic detection

### T9.1 — SmsIngestion (single shared entry point)
- **Purpose:** The one pipeline both real and demo SMS use (R-5.2, design §5).
- **Priority:** P0
- **Files:** `sms/SmsIngestion.kt`, `sms/SmsSource.kt` (REAL, DEMO)
- **Dependencies:** T3.5, T3.7
- **Acceptance:** `ingest(rawText, source)` runs redaction → `check(input_type="sms", source_type="sms_real"|"sms_demo")` → save case; sets `source_type` and writes an audit entry; **no separate result path**.
- **Complexity:** M

### T9.2 — SmsBroadcastReceiver (real, opt-in) + consent
- **Purpose:** Best-effort real SMS detection with per-channel consent (R-5.1, R-5.2.1).
- **Priority:** P0
- **Files:** `sms/SmsBroadcastReceiver.kt`, manifest entry; permission rationale + consent in Automatic Protection
- **Dependencies:** T9.1
- **Acceptance:** With granted `RECEIVE_SMS`, an incoming SMS calls `SmsIngestion.ingest(body, REAL)`; consent required first; toggle off disables receiver; consent change audited.
- **Complexity:** L

### T9.3 — Automatic Protection screen (per-channel toggles)
- **Purpose:** Explicit opt-in mode + privacy explanation (R-5.1, design §8).
- **Priority:** P0
- **Files:** `ui/auto/AutomaticProtectionScreen.kt`, `AutomaticProtectionViewModel.kt`, `data/store/SettingsStore.kt`
- **Dependencies:** T9.2
- **Acceptance:** SMS channel toggle (P0) with rationale/consent; privacy explanation of what's read/redacted/shared; nothing enabled by default; placeholders for Notifications/Calls marked P1 (disabled/"coming").
- **Complexity:** M

---

## PHASE 10 — Local notifications

### T10.1 — RiskNotifier (lock-screen-safe)
- **Purpose:** Local risk alert that opens the case (R-5.3, design §5).
- **Priority:** P0
- **Files:** `notify/RiskNotifier.kt`, notification channel setup
- **Dependencies:** T9.1
- **Acceptance:** On case creation from SMS ingestion, posts a notification with sender masked and content described as threat-masked; tap opens the corresponding Risk Result; used identically by real and demo sources.
- **Complexity:** M

---

## PHASE 11 — Demo simulation fallback + controlled scenarios

### T11.1 — DemoSmsTrigger (same pipeline)
- **Purpose:** Guaranteed P0 fallback via the identical path (R-5.2.2–R-5.2.5, design §5).
- **Priority:** P0
- **Files:** `sms/DemoSmsTrigger.kt`; control in Automatic Protection screen
- **Dependencies:** T9.1, T10.1
- **Acceptance:** Trigger calls `SmsIngestion.ingest(cannedSuspiciousSms, DEMO)`; produces the same notification + Risk Result as real; `source_type="sms_demo"` internally; clearly labeled as demo in-app; never rendered as live monitoring; no separate/fake screen.
- **Complexity:** M

### T11.2 — Deterministic demo scenario data
- **Purpose:** Reproducible live scenarios (R-11.1).
- **Priority:** P0
- **Files:** `data/api/MockSafeCheckApi.kt` (scenario fixtures), `assets/` sample doc/QR
- **Dependencies:** T3.4, T4.x, T5.x, T11.1
- **Acceptance:** Scenarios 1–5 run predictably: High-risk SMS (87/100), Safe message (LOW), URL/QR, Document, Recovery ("I already paid").
- **Complexity:** M

---

## PHASE 12 — Testing + polish

### T12.1 — Accessibility pass (TTS + large text)
- **Purpose:** Accessible results (R-6.5, design §9).
- **Priority:** P0
- **Files:** `accessibility/Tts.kt`; large-text toggle in Privacy/Settings; wire into Risk Result
- **Dependencies:** T3.6
- **Acceptance:** TTS reads band + score + explanation; large-text mode scales result/explanation text.
- **Complexity:** M

### T12.2 — Privacy/Settings screen
- **Purpose:** Explain processing/sharing; controls; audit view (R-8.1.5, design §8).
- **Priority:** P0
- **Files:** `ui/privacy/PrivacyScreen.kt`, `data/store/AuditLog.kt` view
- **Dependencies:** T9.3, T12.1
- **Acceptance:** States what's processed/shared; large-text + consent controls; audit list shows governance events (no secrets/message bodies).
- **Complexity:** M

### T12.3 — Verification & cleanup
- **Purpose:** Confirm acceptance criteria and demo readiness (instruction #29, #34).
- **Priority:** P0
- **Files:** `test/` (redaction, arithmetic, mapping), manual demo checklist in `/docs`
- **Dependencies:** all prior
- **Acceptance:** Redaction masks all spec strings; `ml_pts+url_pts+rule_pts` sums exactly to score in UI; Recovery reachable from result and Home; Safety Circle view never shows raw secrets; no-response defaults to safe; Scenarios 1–5 rehearsed; `assembleDebug` clean; temp files removed.
- **Complexity:** M

---

## P1 — Only after all P0 above is stable

- **T-P1.1** Notification monitoring via `NotificationListenerService` (R-5.4).
- **T-P1.2** Unknown-number call metadata flag (R-5.5).
- **T-P1.3** Explanation translation into one additional language (R-6.5.3).
- **T-P1.4** Richer document intelligence; history/dashboard polish.

## P2 — Future (do not implement)

Deepfake/voice-clone, real WhatsApp interception, real-time video, browser-wide monitoring,
enterprise/family management, production infra, any secret storage, on-device risk engine.

---

## Phase → requirement coverage

| Phase | Delivers | Key requirements |
|---|---|---|
| 0–1 | Build, theme, nav shell | R-1.3 |
| 2 | Home + Manual Check UI | R-2.1, R-3.1/2/5 |
| 3 | **Vertical slice** | R-1.2, R-3.1/2/5, R-6, R-8, R-10.1 |
| 4 | Screenshot/URL/QR | R-3.3, R-3.4, R-11.1.3 |
| 5 | Document | R-3.6, R-11.1.4 |
| 6 | Safety Circle | R-7 |
| 7 | Recovery | R-9 |
| 8 | Real API swap | R-10.1, R-10.2 |
| 9 | Real SMS (single pipeline) | R-5.1, R-5.2 |
| 10 | Notifications | R-5.3 |
| 11 | Demo sim + scenarios | R-5.2, R-11 |
| 12 | Accessibility, privacy, verify | R-6.5, R-8, R-11 |
