package com.safecheck.android.sms

import com.safecheck.android.domain.model.SafetyCase

/**
 * Demo Simulation Mode trigger (requirements R-5.2.2). Simulates a suspicious incoming SMS and
 * runs it through the SAME [SmsIngestion] pipeline as a real SMS, with source = DEMO. Produces
 * the identical redaction -> analysis -> notification -> Risk Result experience.
 *
 * This is clearly a demo control in the UI and is recorded internally as "sms_demo"; it is
 * never represented as real monitoring (requirements R-5.2.4).
 */
class DemoSmsTrigger(private val smsIngestion: SmsIngestion) {

    /** A canned suspicious SMS matching the Master Spec HIGH reference scenario. */
    private val demoSender = "VM-SBIKYC"
    private val demoBody =
        "ALERT: Your bank account will be BLOCKED today. Complete KYC and pay a " +
            "verification fee of Rs.499 now: http://sbi-kyc-update.xyz/verify " +
            "Do not share your OTP 483920 with anyone."

    suspend fun fire(): SafetyCase =
        smsIngestion.ingest(sender = demoSender, body = demoBody, source = SmsSource.DEMO)
}
