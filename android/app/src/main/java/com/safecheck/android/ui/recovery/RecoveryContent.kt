package com.safecheck.android.ui.recovery

import com.safecheck.android.domain.model.IncidentState

/**
 * Scenario selector for targeted triage.
 */
enum class IncidentScenario(val label: String, val badge: String) {
    GENERAL("General", "Standard"),
    MONEY_PAID("Paid Money / UPI", "Financial"),
    CREDENTIAL_SHARED("Shared OTP / PIN", "Identity"),
    MALWARE_INSTALLED("Installed APK / AnyDesk", "Device"),
    LINK_CLICKED("Clicked Link", "Web"),
}

data class RecoveryStageContent(
    val state: IncidentState,
    val title: String,
    val intro: String,
    val actions: List<String>,
    val warning: String? = null,
)

object RecoveryContent {

    val stages: List<RecoveryStageContent> = listOf(
        RecoveryStageContent(
            state = IncidentState.STOP,
            title = "STOP",
            intro = "Cut off the scammer immediately.",
            actions = listOf(
                "Do not click any more links or scan any more codes.",
                "Do not send any more money or share any more details.",
                "Stop replying to the sender and block their phone number.",
            ),
            warning = "If you are currently on a call being instructed to install an app or share a code, hang up immediately.",
        ),
        RecoveryStageContent(
            state = IncidentState.SECURE,
            title = "SECURE",
            intro = "Lock down your money and accounts.",
            actions = listOf(
                "Temporarily freeze UPI and debit card in your banking app.",
                "Request an immediate dispute / transaction freeze with your bank.",
                "Reset passwords of exposed accounts from the official app.",
            ),
            warning = "SafeCheck will never ask you to enter your OTP, PIN, or password.",
        ),
        RecoveryStageContent(
            state = IncidentState.REPORT,
            title = "REPORT",
            intro = "Report the fraud through official Indian government channels.",
            actions = listOf(
                "Call the National Cyber Fraud helpline: 1930 (Golden Hour freeze).",
                "File an official complaint at cybercrime.gov.in.",
                "Optional: RBI Financial Fraud helpline 14440.",
            ),
        ),
        RecoveryStageContent(
            state = IncidentState.DOCUMENT,
            title = "DOCUMENT",
            intro = "Record evidence for your bank dispute or cyber cell report.",
            actions = listOf(
                "Note the exact date, time, and transaction UTR / reference number.",
                "Note the amount and destination UPI ID / account number.",
                "Take screenshots of the message and transaction receipt.",
            ),
            warning = "Never save or send your secret OTPs, PINs, or passwords.",
        ),
        RecoveryStageContent(
            state = IncidentState.LEARN,
            title = "LEARN / PREVENT",
            intro = "Prevent future social engineering attempts.",
            actions = listOf(
                "Real banks/government departments never ask for OTP, PIN, or a 'verification fee'.",
                "Verify accounts only through official apps, never links received via SMS.",
                "Turn on SafeCheck Automatic Protection for SMS to catch threats early.",
            ),
        ),
    )

    fun guidanceForScenario(scenario: IncidentScenario): String = when (scenario) {
        IncidentScenario.MONEY_PAID ->
            "CRITICAL: If money was transferred within the last 2 to 3 hours, call 1930 immediately. Under the Indian Cybercrime Coordination Centre (I4C) protocol, banks can freeze the fraudulent transaction in the beneficiary account."
        IncidentScenario.CREDENTIAL_SHARED ->
            "CRITICAL: If you entered or shared your OTP, Netbanking password, or UPI PIN, open your official banking app right now and change your password or disable netbanking access immediately."
        IncidentScenario.MALWARE_INSTALLED ->
            "CRITICAL: If you installed an APK, AnyDesk, TeamViewer, or RustDesk, immediately turn ON Airplane Mode to cut internet access, then go to Settings -> Apps and uninstall the app."
        IncidentScenario.LINK_CLICKED ->
            "If you only clicked the link but did not enter passwords, OTPs, or download files, clear your browser history and cache, and do not submit any information on that website."
        IncidentScenario.GENERAL ->
            "Follow the 5 recovery stages below step-by-step. Remember to call 1930 or file on cybercrime.gov.in if financial loss occurred."
    }
}

