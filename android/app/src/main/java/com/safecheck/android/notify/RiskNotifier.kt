package com.safecheck.android.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safecheck.android.MainActivity
import com.safecheck.android.R
import com.safecheck.android.domain.model.SafetyCase
import com.safecheck.android.ui.theme.RiskBand

/**
 * Posts local risk notifications (requirements R-5.3). Lock-screen safe: the sender is masked
 * and the content is described only as threat-masked — never the raw message. Tapping opens
 * the corresponding Risk Result. Used identically by real and demo SMS sources.
 */
class RiskNotifier(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SafeCheck alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Alerts when SafeCheck detects a risky message." }
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    /** Post a notification for a case. No-op silently if POST_NOTIFICATIONS is not granted. */
    fun notifyCase(case: SafetyCase) {
        ensureChannel()

        val title = when (case.result.band) {
            RiskBand.HIGH -> "High-risk message detected"
            RiskBand.MEDIUM -> "Suspicious message detected"
            RiskBand.UNCERTAIN -> "Uncertain message — verify safely"
            RiskBand.LOW -> "Message checked"
        }
        // Lock-screen-safe body: no raw content, sender already masked in the case title.
        val body = "Threat content masked on lockscreen for privacy. Tap to review."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CASE_ID, case.caseId)
        }
        val pending = PendingIntent.getActivity(
            context,
            case.caseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(case.caseId.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — the in-app case is still saved and visible.
        }
    }

    companion object {
        const val CHANNEL_ID = "safecheck_alerts"
        const val EXTRA_CASE_ID = "extra_case_id"
    }
}
