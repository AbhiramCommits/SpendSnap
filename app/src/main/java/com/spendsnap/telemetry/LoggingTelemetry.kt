package com.spendsnap.telemetry

import android.util.Log

class LoggingTelemetry : Telemetry {
    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        Log.d(TAG, "event=$name params=$params")
    }

    override fun recordNonFatal(throwable: Throwable) {
        Log.w(TAG, "non-fatal", throwable)
    }

    override fun setUserProperty(
        name: String,
        value: String,
    ) {
        Log.d(TAG, "userProperty=$name value=$value")
    }

    private companion object {
        const val TAG = "SpendSnapTelemetry"
    }
}
