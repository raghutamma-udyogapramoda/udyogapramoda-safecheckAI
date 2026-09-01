# SafeCheck Android — Demo Runbook & Verification Checklist

Use this to rehearse the judge demo and verify acceptance criteria. Default build runs fully
offline against the deterministic mock (`BuildConfig.USE_MOCK_API = true`).

## Build

1. Open `android/` in Android Studio (JDK 17 + Android SDK 34). It regenerates the Gradle wrapper jar on first open.
2. `./gradlew assembleDebug` — build the app.
3. `./gradlew testDebugUnitTest` — run unit tests (RedactionEngine, MockSafeCheckApi).

## Primary "wow" flow — Automatic SMS (Scenario 1)

1. Home → **Automatic Protection**.
2. (Optional real mode) Toggle **SMS automatic detection** → grant `RECEIVE_SMS`.
3. Tap **Simulate suspicious SMS** (guaranteed demo path; identical pipeline to real).
4. Expect: a local notification ("High-risk message detected", content masked) and the app opens the **Risk Result**.
5. Risk Result shows: **HIGH 87/100**, "Score Immutable by LLM", evidence (False Urgency +10, Unauthorized Payment Ask +10, Lookalike Domain 25 URL pts, ML 42), and **ML 42 + URL 25 + Rules 20 = 87**.
6. Tap **Read this aloud** (TTS). 
7. Tap **Ask Safety Circle** → pick "Mom" → **Simulate contact reply** → advisory appears beside the unchanged 87.
8. Back on the result → **Already clicked or paid? Open Recovery** → walk STOP → SECURE → REPORT (1930 / cybercrime.gov.in) → DOCUMENT → LEARN → Finish.

## Manual scenarios

- **Scenario 2 — Safe message:** Manual Check → Text → paste "Your parcel arrives tomorrow 9–11am" → LOW result.
- **Scenario 3 — URL/QR:** Manual Check → URL → `http://secure-login-paypa1.xyz` → MEDIUM, URL evidence + honest "VirusTotal unavailable" banner. Or QR tab → scan/paste.
- **Scenario 4 — Document:** Manual Check → Document → **Use sample document** → key info, deadline, required action, simplified explanation, risk.
- **Scenario 5 — Recovery:** reachable from any result or Home.

## Acceptance checks

- [ ] Redaction: paste "OTP 123456, card 4111 1111 1111 1111, acct 123456789012" → masked before analysis (unit test covers exact outputs).
- [ ] Arithmetic: sub-engine subtotals sum exactly to the displayed score (unit test + on-screen).
- [ ] Privacy: notification shows no raw content; Safety Circle summary shows labels only.
- [ ] Consent: no channel enabled by default; enabling SMS requires permission; toggle changes appear in Privacy → Activity log.
- [ ] Recovery: no OTP/PIN/password stored in incident records.
- [ ] No-response: Safety Circle "Simulate no response" defaults to the safe recommendation.
- [ ] Accessibility: TTS reads the result; Privacy → Large text scales result text.
- [ ] Degradation: with `USE_MOCK_API=false` and no backend, the app falls back to demo data (never fabricates reputation).

## Notes

- The bundled document fallback is `assets/sample_document.txt`; user-picked PDFs use `PdfRenderer` + OCR.
- Real SMS is best-effort (permissions/OEM/emulator dependent). The demo simulation is the guaranteed path and uses the identical `SmsIngestion` pipeline (`source_type = sms_demo`).
