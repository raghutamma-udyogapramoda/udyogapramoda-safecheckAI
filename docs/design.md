# SafeCheck Android — Design Specification

**Developer:** Developer 1 (Android client only)
**Source of truth:** SafeCheck Unified Master Specification
**Companion:** `requirements.md` (approved)
**Status:** Draft for review (do not start tasks.md until approved)

---

## 0. How to read this document

This document defines **HOW** the Android client is built. It maps every approved P0
requirement to concrete architecture, modules, data models, and screens. It deliberately
avoids duplicating backend "brain" logic.

Guiding rule from the Master Spec: **ONE PRODUCT + ONE SAFETY BRAIN + TWO PLATFORM
EXPERIENCES.** Android is a *client*. The only "intelligence" on-device is (a) PII redaction
and (b) local extraction (OCR/QR/PDF text) — both of which only *prepare* input for the
shared API. The verdict always comes from the backend.

---

## 1. Technology choices (simplest reliable stack)

| Concern | Choice | Rationale |
|---|---|---|
| Language | Kotlin | Standard for modern Android; concise; coroutines. |
| Min / Target SDK | min 26 (Android 8) / target 34 | Covers notification channels, modern share sheet; broad device support. |
| UI | Jetpack Compose + Material 3 | Fast to build cards/navigation; themable for SafeCheck brand. |
| Navigation | Navigation-Compose (single-Activity) | Mobile step-based navigation per spec §26. |
| Async | Kotlin Coroutines + Flow | Simple state + one-shot API calls. |
| DI | Manual constructor injection via a small `AppContainer` | Avoids Hilt setup cost for a 2.5-day build; still testable. |
| Networking | Retrofit + OkHttp + kotlinx.serialization | Real client behind an interface; **mock impl used until backend ready**. |
| Local persistence | DataStore (settings/consent) + a small Room DB (cases/audit) | Minimal retention; no secrets stored. |
| QR | ML Kit Barcode Scanning + CameraX | On-device decode before opening. |
| OCR | ML Kit Text Recognition | On-device screenshot/image text extraction. |
| PDF text | Android `PdfRenderer` + text heuristic, with **bundled sample PDF fallback** | P0 = text extract or fallback; heavy scanned-PDF OCR is not P0. |
| TTS | Android `TextToSpeech` | Accessibility on results. |
| Notifications | `NotificationManager` + channel | Local risk alerts, lock-screen safe. |

No feature above introduces backend risk logic on-device.

---

## 2. High-level architecture

Clean-ish layering, kept lightweight for the timeline. Three layers:

```
┌─────────────────────────────────────────────────────────────┐
│  UI LAYER (Compose)                                          │
│  Screens + ViewModels (state holders)                        │
│  Home · ManualCheck · RiskResult · AutomaticProtection ·     │
│  SafetyCircle · Recovery · Privacy/Settings                  │
└───────────────▲──────────────────────────┬──────────────────┘
                │ UiState (Flow)            │ intents/events
┌───────────────┴──────────────────────────▼──────────────────┐
│  DOMAIN LAYER (pure Kotlin, no Android deps where possible)  │
│  • AnalyzeContentUseCase   • SubmitDocumentUseCase           │
│  • ShareToSafetyCircleUseCase / SubmitReviewUseCase          │
│  • RecordRecoveryIncidentUseCase                             │
│  • RedactionEngine (on-device PII masking)  ← the ONLY       │
│    "brain-adjacent" on-device logic besides extraction       │
│  • Models: SafetyCase, Evidence, RiskResult, Incident, ...   │
└───────────────▲──────────────────────────┬──────────────────┘
                │                           │
┌───────────────┴──────────────────────────▼──────────────────┐
│  DATA / PLATFORM LAYER                                        │
│  • SafeCheckApi (interface)                                  │
│      ├─ MockSafeCheckApi (deterministic, spec-accurate)      │
│      └─ RetrofitSafeCheckApi (real, behind config flag)      │
│  • Extraction: QrScanner, OcrExtractor, PdfTextExtractor     │
│  • Sms: SmsIngestion (single path) ← real + demo sources     │
│  • Notifications: RiskNotifier                               │
│  • Persistence: CaseStore (Room), SettingsStore (DataStore), │
│    AuditLog (Room)                                           │
└──────────────────────────────────────────────────────────────┘
```

**Boundary guarantees:**
- The UI and Domain layers never see raw un-redacted content after the redaction choke-point.
- The Domain layer talks only to `SafeCheckApi` (interface). It does not know if the impl is mock or real.
- No class computes a final risk score. `RiskResult.score/level` are read from the API response only.

---

## 3. On-device privacy: the redaction choke-point

**Design principle:** there is exactly **one** function every piece of outbound content must pass
through before it can be sent to the API, logged, or notified. This makes AC-8.1.x auditable.

```
RawInput ──▶ RedactionEngine.redact(text) ──▶ RedactedContent ──▶ [API | Log | Notification]
                        │
                        └── produces: maskedText + list<RedactionHit>(type only, no value)
```

`RedactionEngine` (pure Kotlin, unit-testable) masks with deterministic rules:

| Type | Detection (regex-based) | Mask rule |
|---|---|---|
| OTP | 4–8 digit standalone code near "OTP/code" context | full mask `******` |
| Card number | 13–19 digits (Luhn-ish grouping) | keep first 4 + last 4, mask middle |
| Bank account | 9–18 digit run | keep last 4, mask rest |
| IFSC | `[A-Z]{4}0[A-Z0-9]{6}` | full mask |
| UPI ID | `[\w.\-]+@[\w]+` (VPA-style) | mask local part |
| PAN | `[A-Z]{5}[0-9]{4}[A-Z]` | full mask |
| Aadhaar | 12-digit run | keep last 4, mask rest |
| Password/PIN | labeled fields / "PIN is 1234" context | full mask |

Notes:
- Applied to **all** sources: manual text, email body, OCR output, SMS body, document text.
- `RedactionHit` records **type only** (e.g., `AADHAAR`), never the raw value → safe for audit/evidence.
- Emphasis on **no false negatives on the demo strings** from the spec (the exact examples must mask correctly). This is a targeted, deterministic engine, not a general PII ML model.

---

## 4. Shared API boundary (mock-first)

`SafeCheckApi` mirrors the Master Spec §27 contract exactly:

```kotlin
interface SafeCheckApi {
    suspend fun check(request: CheckRequest): CheckResponse            // POST /v1/check
    suspend fun document(request: DocumentRequest): DocumentResponse   // POST /v1/document
    suspend fun shareToCircle(request: ShareRequest): ShareResponse    // POST /v1/safety-circle/share
    suspend fun submitReview(request: ReviewRequest): ReviewResponse   // POST /v1/safety-circle/review
    suspend fun recordIncident(request: IncidentRequest): IncidentResponse // POST /v1/recovery/incident
    suspend fun history(): List<SafetyCaseSummary>                     // GET  /v1/history
}
```

**Request/response DTOs** follow the spec concept:

```
CheckRequest  { input_type: "sms|text|screenshot|url|qr|document|email", content: String,
                source_type: String, redaction_hits: List<String> /* types only */ }
CheckResponse { case_id, risk_score: Int, risk_level: "LOW|MEDIUM|HIGH",
                evidence: [ { sub_engine, label, points, observed_value, confidence } ],
                sub_scores: { ml_pts, url_pts, rule_pts },
                explanation: String, recommended_actions: [String],
                unavailable_signals: [String],   // honest "signal unavailable"
                model_versions: { rule_version, model_version, prompt_version } }
```

**Two implementations, one flag:**
- `MockSafeCheckApi` — deterministic. Recognizes the demo scenarios and returns spec-accurate
  results, most importantly the HIGH case: `ml_pts=42, url_pts=25, rule_pts=20 → 87/100 HIGH`,
  with evidence items (False Urgency +10, Unauthorized Payment Ask +10, Lookalike Domain 25 URL pts)
  that **sum exactly** to the score. Also a clearly-LOW safe case.
- `RetrofitSafeCheckApi` — real HTTP, same interface.
- Selected in `AppContainer` via a single `BuildConfig`/config flag. **No domain or UI change** when swapping.

**Degradation (AC-10.2):** the mock/real client can return `unavailable_signals`; the Risk Card
renders these honestly ("VirusTotal check unavailable") and never fabricates a verdict. On total
failure, the app falls back to controlled deterministic demo data.

---

## 5. SMS: single ingestion path, two sources (real + demo)

This is the design realization of the approved clarification. **Real and demo SMS are the same
pipeline**; they differ only by an internal `source_type` marker.

```
        REAL SOURCE                         DEMO SOURCE
   SmsBroadcastReceiver                DemoSmsTrigger (in-app,
   (RECEIVE_SMS, opt-in)               labeled control in
        │                              Automatic Protection)
        │  raw sms text                     │  canned suspicious sms text
        ▼                                   ▼
        └───────────────┬───────────────────┘
                        ▼
              SmsIngestion.ingest(rawText, source)   ← SINGLE ENTRY POINT
                        │   source = SmsSource.REAL | SmsSource.DEMO
                        ▼
              RedactionEngine.redact(rawText)        ← same privacy path
                        ▼
              AnalyzeContentUseCase(input_type="sms",
                    source_type = "sms_real" | "sms_demo")
                        ▼
              SafeCheckApi.check(...)                ← same shared brain call
                        ▼
              CaseStore.save(SafetyCase)             ← same case model
                        ▼
              RiskNotifier.notify(case)              ← same notification component
                        ▼
        (tap) → RiskResultScreen(case_id)            ← same Risk Result UI
```

**Guarantees enforced by design:**
- There is exactly one `SmsIngestion.ingest(...)`; both `SmsBroadcastReceiver` and `DemoSmsTrigger`
  call it. No alternate/fake result screen exists.
- `SafetyCase.source_type` carries `sms_real` vs `sms_demo`; the `AuditLog` records which fired.
- The judge-facing screens (notification + Risk Result) are byte-for-byte the same components.
- Real mode is best-effort (permission + device dependent). Demo mode is always available and
  produces the identical experience — guaranteeing the "wow" flow demos regardless of device.
- Internally-honest, externally-coherent: nowhere is `sms_demo` labeled to the user as live
  monitoring; nowhere does the demo path diverge into a separate UI.

---

## 6. Extraction modules (on-device, prepare-only)

- **QrScanner** (CameraX + ML Kit): decode payload on-device → pass decoded string into the same
  redaction + `check(input_type="qr")` path. Never auto-opens the target.
- **OcrExtractor** (ML Kit Text Recognition): image → text → redaction → `check(input_type="screenshot")`.
  Failure surfaces an honest error + manual-paste fallback.
- **PdfTextExtractor** (`PdfRenderer` + text heuristic): PDF → text → `document(...)`. If extraction
  yields too little text, use the **bundled sample document** so the journey still demos (AC-3.6.1).

All extractors output plain text that immediately enters the redaction choke-point (Section 3).

---

## 7. Domain models (aligned to Master Spec §28)

```
SafetyCase       { case_id, type/input_type, timestamp, source_type, risk_band, score,
                   sub_scores, evidence: List<Evidence>, explanation,
                   recommended_actions, unavailable_signals, review: Review?, incident: Incident? }
Evidence         { evidence_id, sub_engine, label, points, observed_value, confidence }
Review           { case_id, reviewer_id, decision, timestamp, note }   // Safety Circle advisory
Incident         { case_id, incident_state, recovery_actions, outcome } // Recovery (no secrets)
TrustedContact   { contact_id, name, relationship, verified_channel }   // local/mock ok
AuditLogEntry    { actor, action_type, timestamp, policy_version }      // governance only
```

Storage respects minimal retention (AC-8.2): raw content is not persisted; `SafetyCase` stores the
sanitized result; `AuditLog` stores governance facts only.

---

## 8. Screens & navigation (mobile-native)

Single-Activity, bottom navigation with 4 primary destinations + detail routes.

**Bottom nav:** Home · Protection · Circle · Recovery. (Privacy/Settings via Home + Protection.)

| Screen | Purpose | Key UI states |
|---|---|---|
| **Home** | Branding, protection status, entry points, recent activity | loaded / empty recent list |
| **Manual Check** | Tabs: Text · URL · QR · Screenshot · Email · Document | idle / capturing / analyzing / error |
| **Risk Result** | Risk Card, Evidence (with arithmetic), Explanation, Safe Actions, TTS, Share, Recovery entry | loaded / signal-unavailable / offline banner |
| **Automatic Protection** | Per-channel toggles (SMS P0; Notifications/Calls P1), rationale + consent, **Demo SMS trigger**, privacy explanation | channel off/on / permission-denied |
| **Safety Circle** | Pick contact, sanitized summary preview, send, advisory view | no contacts / awaiting / advisory shown / no-response default |
| **Recovery** | 5-stage wizard STOP→SECURE→REPORT→DOCUMENT→LEARN | per-stage progress / completion |
| **Privacy/Settings** | What's processed/shared, large-text toggle, consent controls, audit view | — |

**Cross-cutting UI states** (from review item #8): global analyzing/loading, empty states,
permission-denied (camera, SMS), external-signal-unavailable chip on Risk Card, backend-down banner.

**Design system:** SafeCheck theme (brand colors, risk-band color tokens LOW=green, MEDIUM=amber,
HIGH=red), reusable `RiskCard`, `EvidenceRow`, `SafeCheckButton`, `ChannelToggle`, `StageStepper`,
large-text scaling hook. Terminology fixed to spec.

---

## 9. Accessibility design

- **TTS**: `TextToSpeech` reads band + score + explanation on the Risk Result screen via a speaker button.
- **Large text**: a setting multiplies Compose font scale across result/explanation screens.
- **Plain language**: explanation text comes from the API's LLM layer; UI adds no jargon.
- **Translation (P1)**: explanation string is display-only, so a future translated field slots in
  without structural change.

---

## 10. Safety Circle design

- Local/mock `TrustedContact` list (P0). Share builds a **sanitized summary** from `SafetyCase`
  (band, score, evidence labels only — no raw values, no secrets) via `shareToCircle(...)`.
- Advisory `Review` is displayed **beside** the immutable score. Simulated advisory acceptable
  (clearly labeled). No-response → app shows the existing safe recommendation (AC-7.1.4).

---

## 11. Recovery design

- Reachable from any Risk Result and from Home ("I already clicked / paid / shared").
- `StageStepper` drives STOP → SECURE → REPORT → DOCUMENT → LEARN/PREVENT with immediate actions,
  warnings, progress, completion.
- REPORT surfaces 1930 / cybercrime.gov.in / RBI 14440.
- DOCUMENT builds an `Incident` summary via `recordIncident(...)`; redaction choke-point guarantees
  zero OTP/PIN/password in the record.

---

## 12. Design self-review (against instruction #28)

- **Android/Web separation:** only Android code; no web artifacts. ✔
- **Client-only role:** verdict fields read from API; no local scoring. ✔
- **No duplicate risk engine:** on-device logic limited to redaction + extraction. ✔
- **Privacy boundary clear:** single redaction choke-point before any send/log/notify. ✔
- **API boundary clear:** one `SafeCheckApi` interface, mock/real swap via one flag. ✔
- **SMS isolated + single path:** real + demo converge on `SmsIngestion.ingest`; internal source
  marker; shared notification + Risk Result UI; no separate fake screen. ✔
- **Demo fallback exists:** deterministic mock + Demo SMS + sample document + honest unavailable signals. ✔
- **P0 realistic for 2.5 days:** manual constructor DI, mock-first, narrowed document/email/share scope. ✔
- **Not over-engineered:** no Hilt, no multi-module gradle, minimal Room usage. ✔

---

## 13. Traceability (requirements → design)

| Requirement | Design section |
|---|---|
| R-1.2 client-only, no local scoring | §2 boundary guarantees, §4, §7 |
| R-3.1–R-3.7 inputs | §6 extraction, §8 Manual Check |
| R-5.2 SMS real+demo single path | §5 |
| R-5.3 notification | §5 `RiskNotifier`, §8 |
| R-6 Risk Result + evidence arithmetic + TTS | §4 DTOs, §8, §9 |
| R-7 Safety Circle | §10 |
| R-8 privacy/redaction + retention | §3, §7 |
| R-9 Recovery | §11 |
| R-10 API mock-first + degradation | §4 |
| R-11 demo scenarios | §4 mock, §5 demo SMS, §6 sample doc |
