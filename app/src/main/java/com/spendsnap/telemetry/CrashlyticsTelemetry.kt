package com.spendsnap.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed telemetry. A clone without google-services.json has no
 * FirebaseApp, so all calls fall back to logcat instead of crashing on startup.
 */
@Singleton
class CrashlyticsTelemetry
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Telemetry {
        private val delegate: Telemetry by lazy {
            if (FirebaseApp.getApps(context).isEmpty()) {
                LoggingTelemetry()
            } else {
                FirebaseTelemetry(context)
            }
        }

        override fun logEvent(
            name: String,
            params: Map<String, Any>,
        ) = delegate.logEvent(name, params)

        override fun recordNonFatal(throwable: Throwable) = delegate.recordNonFatal(throwable)

        override fun setUserProperty(
            name: String,
            value: String,
        ) = delegate.setUserProperty(name, value)
    }

private class FirebaseTelemetry(context: Context) : Telemetry {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        analytics.logEvent(name, params.toBundle())
    }

    override fun recordNonFatal(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun setUserProperty(
        name: String,
        value: String,
    ) {
        analytics.setUserProperty(name, value)
    }

    private fun Map<String, Any>.toBundle(): Bundle =
        Bundle().apply {
            forEach { (key, value) ->
                when (value) {
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
}
