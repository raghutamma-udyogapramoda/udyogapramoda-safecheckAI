package com.safecheck.android.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.safecheck.android.SafeCheckApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Real Device Mode SMS listener (requirements R-5.2.1). Registered dynamically ONLY after the
 * user opts in and grants RECEIVE_SMS on the Automatic Protection screen — never by default.
 *
 * It reads the incoming SMS body (a structured system event — no WhatsApp/chat scraping,
 * Master Spec §10) and hands it to the SAME [SmsIngestion] pipeline as the demo trigger, with
 * source = REAL. Best-effort by design: if this never fires (permissions, OEM, emulator), the
 * Demo Simulation path guarantees the journey still works.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().displayOriginatingAddress ?: "Unknown"
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        if (body.isBlank()) return

        val app = context.applicationContext as? SafeCheckApp ?: return
        val ingestion = app.container.smsIngestion
        val settings = app.container.settingsStore

        // Keep the process alive briefly while the async analysis + notification run.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settings.isSmsChannelEnabled()) {
                    ingestion.ingest(sender = maskSender(sender), body = body, source = SmsSource.REAL)
                }
            } catch (_: Throwable) {
                // Best-effort: never crash on a background SMS.
            } finally {
                pending.finish()
            }
        }
    }

    /** Sender is shown only in a masked/short form; the raw body is redacted downstream. */
    private fun maskSender(sender: String): String =
        if (sender.length <= 4) sender else sender.take(2) + "••••" + sender.takeLast(2)
}
