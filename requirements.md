# SafeCheck Android — Requirements Specification

**Developer:** Developer 1 (Android client only)
**Source of truth:** SafeCheck Unified Master Specification (the Master Spec)
**Timeline:** ~2.5 days, one developer
**Status:** Draft for review (do not start design until approved)

---

## 0. How to read this document

This document defines **WHAT** the SafeCheck Android client must do. It does not define
implementation (that is `design.md`) or task order (that is `tasks.md`).

Every requirement is tagged with a priority:

- **P0** — must work for a live judge demo. Non-negotiable.
- **P1** — only after all P0 is stable.
- **P2** — future / explicitly out of scope for this build.

Requirements use the form: *As a [user], I want [capability], so that [value]*, followed by
numbered **acceptance criteria (AC)** that are objectively verifiable.

---

## 1. Product framing (boundaries)

**R-1.1 (P0) — One product, two clients.** SafeCheck is ONE product with a shared backend
("Safety Brain") and two clients (Web, Android). This project builds **only the Android client**.

- AC-1.1.1 No web application code exists in this workspace.
- AC-1.1.2 No second/competing product concept is introduced.
- AC-1.1.3 Android source is self-contained and integrates with the shared API via a single, versioned client boundary.

**R-1.2 (P0) — Android is a client of the shared brain, not a copy of it.** The Android app
MUST NOT compute the final risk score or embed the deterministic risk engine, rule engine,
URL intelligence, or ML classifier.

- AC-1.2.1 The final `risk_score` and `risk_level` displayed always originate from the shared API (real or mock), never from local calculation.
- AC-1.2.2 The app contains no local re-implementation of `Score = min(100, ML_pts + URL_pts + Rule_pts)`.
- AC-1.2.3 The only "brain-like" logic on-device is **PII redaction** (privacy pre-pass) and **local extraction** (OCR/QR/PDF text), both of which prepare input for the shared API.

**R-1.3 (P0) — Unified design language.** The Android UI uses SafeCheck branding, terminology
(Risk Card, Evidence, Explanation, Safety Circle, Recovery, Manual Check, Automatic Protection),
risk levels (LOW/MEDIUM/HIGH), score format (`X/100`, "Score Immutable by LLM"), and the
privacy tagline "Your Privacy. Your Safety. Always in Control."

- AC-1.3.1 Terminology matches the Master Spec exactly; no synonyms invented.
- AC-1.3.2 Presentation is mobile-native (cards, touch controls, bottom navigation, native permission prompts), not a desktop dashboard ported to phone.

---

## 2. Home / Protection Status

**R-2.1 (P0) — Home screen.** As a user, I want a home screen that shows my protection status
and the main actions, so that I can start any journey in one tap.

- AC-2.1.1 Home shows SafeCheck branding and an overall protection status summary (e.g., "Protected" / "Manual only").
- AC-2.1.2 Home surfaces primary entry points: **Manual Check**, **Automatic Protection**, **Safety Circle**, **Recovery**, **Privacy/Settings**.
- AC-2.1.3 Home shows a **recent activity** list (recent cases/alerts) where practical; empty state is handled gracefully.
- AC-2.1.4 The mode choice (Manual Check vs Automatic Protection) is explicit and user-driven.

---

## 3. Manual Check (input capture)

**R-3.1 (P0) — Text / paste.** As a user, I want to paste or type suspicious text and check it.

- AC-3.1.1 A multiline text input accepts pasted content and submits it for analysis.
- AC-3.1.2 On submit, local redaction runs before the content leaves the device (see R-8).

**R-3.2 (P0) — URL.** As a user, I want to submit a URL for reputation analysis.

- AC-3.2.1 A URL field accepts a typed/pasted link and submits it as `input_type: "url"`.
- AC-3.2.2 The app **never opens the URL** to test it; it only sends it to the shared API.

**R-3.3 (P0) — QR code (native camera).** As a user, I want to scan a QR code with my camera.

- AC-3.3.1 Camera-based QR scanning decodes the payload **on-device** before it is opened or sent.
- AC-3.3.2 A decoded URL/payload is submitted as `input_type: "qr"`; the target is never auto-opened.
- AC-3.3.3 Camera-permission denial degrades gracefully to a manual fallback (paste the QR content).

**R-3.4 (P0) — Screenshot / image (OCR).** As a user, I want to check a screenshot or photo.

- AC-3.4.1 The user can select an image; text is extracted via on-device OCR.
- AC-3.4.2 Extracted text is redacted (R-8) before submission as `input_type: "screenshot"`.
- AC-3.4.3 OCR failure is shown honestly with a manual-paste fallback (per Master Spec §15.7 / instruction #22).

**R-3.5 (P0) — Email / content.** As a user, I want to check email content (sender/subject/body).

- AC-3.5.1 The user can submit email-style content as `input_type: "email"`.
- AC-3.5.2 Sender/domain is included so the backend can perform lookalike-domain analysis.

**R-3.6 (P0) — Document / PDF (core journey).** As a user, I want to check a PDF/document.

- AC-3.6.1 The user can select a PDF; the app extracts text (with a controlled sample-document fallback if extraction fails, per instruction #17).
- AC-3.6.2 The document result surfaces: key information, deadlines, required actions, suspicious URLs/payment/contact info, simplified explanation, and risk where applicable.
- AC-3.6.3 Submitted as `input_type: "document"` to the document endpoint.

**R-3.7 (P0) — Share-to-SafeCheck.** As a user, I want to share content from another app
(including a WhatsApp screenshot or copied text) into SafeCheck.

- AC-3.7.1 SafeCheck registers as a share target for text and images.
- AC-3.7.2 Shared content enters the same Manual Check → redaction → analysis pipeline.

---

## 4. WhatsApp scope (constraint, not a feature)

**R-4.1 (P0) — WhatsApp is manual/shared only.** SafeCheck supports WhatsApp content **only**
via explicit user action (screenshot, paste, or Android share).

- AC-4.1.1 No background WhatsApp interception, no message-body scraping, no accessibility-based scraping exists.
- AC-4.1.2 No UI text claims SafeCheck automatically reads WhatsApp conversations.

---

## 5. Automatic Protection (opt-in per channel)

**R-5.1 (P0) — Explicit mode + per-channel consent.** As a user, I want automatic protection
to be opt-in per channel with clear rationale, so that nothing monitors me silently.

- AC-5.1.1 Automatic Protection is a distinct mode, selected explicitly.
- AC-5.1.2 Each channel (SMS = P0; Notifications, Unknown-number calls = P1) has an independent toggle.
- AC-5.1.3 Enabling a channel shows a permission rationale and requires explicit consent before any permission request.
- AC-5.1.4 A clear privacy explanation states what is read (structured SMS text only), what is redacted, and what is shared.
- AC-5.1.5 No channel is enabled by default.
- AC-5.1.6 Consent changes and channel enable/disable are recorded to a local audit log (governance facts only; no message content).

**R-5.2 (P0) — SMS automatic detection (two paths).** As a user, when a suspicious SMS-type
event occurs, I want an automatic, privacy-preserving alert.

- AC-5.2.1 **Real Device Mode:** with granted SMS permission, an incoming SMS triggers local redaction → shared analysis → local notification → opens the SafeCheck case.
- AC-5.2.2 **Demo Simulation Mode:** an internal, clearly-labeled trigger simulates a suspicious incoming SMS and follows the **identical** journey and notification behavior.
- AC-5.2.3 **Single shared path.** Both modes feed the **same** SMS ingestion entry point and therefore reuse the identical application state flow, privacy/redaction path, `SafetyCase` model, notification components, and Risk Result UI. Neither mode uses a separate fake UI or a disconnected demo screen.
- AC-5.2.4 **Internal source distinction.** The true origin is recorded internally on the case as `source_type = "sms_real"` vs `"sms_demo"` (and in the local audit log), while the judge-facing experience remains coherent and identical across both. Demo Simulation Mode is never represented internally as real monitoring.
- AC-5.2.5 If SMS permission or real interception is unavailable, the product remains fully demoable via Demo Simulation Mode with no change to the user-facing journey.

**R-5.3 (P0) — Local risk notification.** As a user, I want a lock-screen-safe alert.

- AC-5.3.1 The notification masks sender and describes content as threat-masked for privacy (no raw content on lock screen).
- AC-5.3.2 Tapping the notification opens the corresponding Risk Result / case.

**R-5.4 (P1) — Notification monitoring.** Scanning UPI/Gmail notification text via
`NotificationListenerService`, opt-in, feasibility permitting.

**R-5.5 (P1) — Unknown-number call metadata.** Flag unknown-number calls by number/time only,
never audio. Opt-in.

---

## 6. Risk Result & Evidence

**R-6.1 (P0) — Risk Card.** As a user, I want a clear risk verdict.

- AC-6.1.1 Displays risk band (LOW/MEDIUM/HIGH) and score as `X/100`.
- AC-6.1.2 Displays the label "Score Immutable by LLM."
- AC-6.1.3 Color/semantics: LOW 0–39, MEDIUM 40–69, HIGH 70–100.

**R-6.2 (P0) — Evidence with arithmetic.** As a user, I want to see why the score is what it is.

- AC-6.2.1 Evidence lists itemized signals with points (e.g., False Urgency +10, Unauthorized Payment Ask +10, Lookalike Domain 25 URL pts).
- AC-6.2.2 Sub-engine subtotals (ML_pts + URL_pts + Rule_pts) are shown and **sum exactly** to the displayed score.
- AC-6.2.3 Evidence distinguishes observed facts, inferred signals, and unknowns; unavailable external signals are shown honestly, never fabricated.

**R-6.3 (P0) — Plain-language explanation.** As a user, I want a non-technical explanation.

- AC-6.3.1 The explanation (from the downstream LLM via the API) is rendered as plain language.
- AC-6.3.2 The UI makes clear the explanation does not set the score.

**R-6.4 (P0) — Safe Action guidance.** Every result answers: what happened, why it's risky, what to do now.

- AC-6.4.1 Shows recommended actions from the API (e.g., do not click, do not pay, verify via official channel).
- AC-6.4.2 Provides a Safety Circle action and a Recovery entry ("Already Clicked or Paid?").

**R-6.5 (P0) — Accessibility on results.** As a user with access needs, I want audio + large text.

- AC-6.5.1 TTS reads the risk band, score, and explanation.
- AC-6.5.2 A large-text mode increases readable text sizes across result screens.
- AC-6.5.3 (P1) Optional translation of the explanation into one additional language.

---

## 7. Safety Circle

**R-7.1 (P0) — Advisory second opinion.** As a user with a high/uncertain-risk case, I want to
ask a trusted contact.

- AC-7.1.1 The user can pick a trusted contact (local/mock contacts acceptable for the hackathon).
- AC-7.1.2 Only a **sanitized** case summary is shared — no OTPs, PINs, passwords, or raw personal logs.
- AC-7.1.3 An advisory opinion (real or simulated, clearly labeled) is displayed **alongside** but never replaces the machine score.
- AC-7.1.4 If no response, the app defaults to the safe recommendation already shown; it never implies approval.
- AC-7.1.5 The user makes the final decision (Block & Finish, or Proceed/Recovery); a contact can never take over the decision.

---

## 8. Privacy & On-Device Redaction

**R-8.1 (P0) — Redact before send.** As a user, I want sensitive data masked on-device before
anything is transmitted.

- AC-8.1.1 The following are detected and masked locally before any network call or logging: OTP, card numbers, bank account numbers, IFSC, UPI IDs, PAN, Aadhaar, passwords, PINs, and comparable secrets.
- AC-8.1.2 Masking matches the spec examples: `123456 → ******`, `4111 1111 1111 1111 → 4111 **** **** 1111`, `123456789012 → ********9012`.
- AC-8.1.3 The app never requests passwords, OTPs, or PINs.
- AC-8.1.4 The app never intentionally stores these secrets (including in Recovery records, evidence, notifications, or logs).
- AC-8.1.5 A privacy screen explains what is processed and what is shared, and gives control over monitoring.

**R-8.2 (P0) — Minimal retention.** Data is structured into Raw Content (shortest retention),
Sanitized Evidence (as needed to explain/audit), and Security Metadata (may persist for audit).

- AC-8.2.1 Raw captured content is not persisted beyond the active analysis.
- AC-8.2.2 Displayed evidence is sanitized.
- AC-8.2.3 Local audit log stores governance facts only (no raw secrets or message bodies).

---

## 9. Recovery

**R-9.1 (P0) — Guided recovery.** As a user who already acted, I want a guided recovery flow.

- AC-9.1.1 Entry is reachable from any risk result and from a fresh start ("I already clicked / paid / shared").
- AC-9.1.2 Interactive five-stage wizard: STOP → SECURE → REPORT → DOCUMENT → LEARN/PREVENT.
- AC-9.1.3 REPORT surfaces the correct helplines (1930 / cybercrime.gov.in; RBI 14440 as additional).
- AC-9.1.4 DOCUMENT produces an incident summary under a strict zero OTP/PIN/password rule.
- AC-9.1.5 The flow shows immediate actions, warnings, progress, and a completion state.

---

## 10. Shared API integration

**R-10.1 (P0) — Single client boundary + mock-first.** As the developer, I want one API
boundary with a mock implementation, so I can build before the backend is ready.

- AC-10.1.1 A single API interface represents the shared contract: `POST /v1/check`, `POST /v1/document`, `POST /v1/safety-circle/share`, `POST /v1/safety-circle/review`, `POST /v1/recovery/incident`, `GET /v1/history`.
- AC-10.1.2 Request/response shapes match the Master Spec data model (SafetyCase, Evidence, Review, Incident, etc.).
- AC-10.1.3 A deterministic mock implementation returns spec-accurate results and is swappable to a real HTTP client via a single config flag, with no UI/domain changes.
- AC-10.1.4 The Android app does not invent an API contract that conflicts with the Master Spec.

**R-10.2 (P0) — Graceful degradation.** As a user, I want the app to keep working when services fail.

- AC-10.2.1 Backend/network/LLM/OCR/camera/permission failures never crash the journey.
- AC-10.2.2 Unavailable external signals are shown honestly; the app never fabricates a reputation result.
- AC-10.2.3 On total backend unavailability, controlled demo data keeps the main journey functional and clearly deterministic.

---

## 11. Demo scenarios (must be reproducible live)

**R-11.1 (P0) — Deterministic demo scenarios.** The following must run predictably on demand:

- AC-11.1.1 **Scenario 1 — High-risk SMS:** suspicious SMS (real or demo sim) → redaction → notification → HIGH result (target 87/100 = ML 42 + URL 25 + Rules 20) → evidence → explanation → DO NOT CLICK/PAY → Safety Circle.
- AC-11.1.2 **Scenario 2 — Safe message:** normal message → LOW result → safe explanation.
- AC-11.1.3 **Scenario 3 — URL/QR:** suspicious URL or QR → extraction → URL/domain intelligence evidence → result → safe action.
- AC-11.1.4 **Scenario 4 — Document:** sample document → extraction → key info + deadline/action → simplified explanation → risk where applicable.
- AC-11.1.5 **Scenario 5 — Recovery:** "I already paid" → STOP → SECURE → REPORT → DOCUMENT → LEARN/PREVENT.

---

## 12. Explicitly out of scope (P2 / future)

- Deepfake / voice-clone detection
- Real WhatsApp interception or message-body scraping
- Real-time video call analysis
- Browser-wide monitoring
- Enterprise/family policy management beyond a single Safety Circle contact
- Production-scale infrastructure
- Any feature requiring storage of OTPs, PINs, passwords, or unredacted secrets
- On-device reimplementation of the deterministic risk engine or threat-intelligence sources

---

## 13. Priority rollup

**P0 (must work):** R-1.1, R-1.2, R-1.3, R-2.1, R-3.1–R-3.7, R-4.1, R-5.1, R-5.2, R-5.3,
R-6.1–R-6.5 (TTS/large text), R-7.1, R-8.1, R-8.2, R-9.1, R-10.1, R-10.2, R-11.1.

**P1 (after P0 stable):** R-5.4 (notification monitoring), R-5.5 (call metadata),
R-6.5.3 (translation), richer document intelligence, history polish.

**P2 (future):** Section 12.

---

## 14. Success criterion (product-level)

A judge can experience, as one coherent SafeCheck system:
HOME → (Manual Check OR Automatic Protection) → content detected → privacy/redaction →
analysis → risk score → evidence → plain-language explanation → safe action → Safety Circle → Recovery.

Primary "wow" flow: suspicious SMS → automatic detection (real or approved simulation) →
privacy-preserving alert → HIGH risk → evidence → explanation → Safety Circle → Recovery.
