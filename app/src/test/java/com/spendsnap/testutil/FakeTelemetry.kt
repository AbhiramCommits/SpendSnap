package com.spendsnap.testutil

import com.spendsnap.telemetry.Telemetry

class FakeTelemetry : Telemetry {
    data class Event(val name: String, val params: Map<String, Any>)

    val events = mutableListOf<Event>()
    val nonFatals = mutableListOf<Throwable>()
    val userProperties = mutableMapOf<String, String>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        events += Event(name, params)
    }

    override fun recordNonFatal(throwable: Throwable) {
        nonFatals += throwable
    }

    override fun setUserProperty(
        name: String,
        value: String,
    ) {
        userProperties[name] = value
    }
}
