package com.spendsnap.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashlyticsTelemetryTest {
    @Test
    fun missingFirebaseConfig_degradesToLoggingWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val telemetry = CrashlyticsTelemetry(context)

        telemetry.logEvent(TelemetryEvents.EXPENSE_CREATED, mapOf("amount_cents" to 100))
        telemetry.recordNonFatal(IllegalStateException("expected test non-fatal"))
        telemetry.setUserProperty("tier", "internal-test")
    }
}
