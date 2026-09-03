package com.safecheck.android.sms

/**
 * Origin of an SMS ingested by [SmsIngestion]. Both sources flow through the SAME pipeline;
 * this only records the true provenance internally (requirements R-5.2.4). It is NEVER used
 * to present demo events as real monitoring.
 */
enum class SmsSource(val sourceType: String) {
    REAL("sms_real"),
    DEMO("sms_demo"),
}
