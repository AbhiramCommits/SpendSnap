package com.spendsnap.telemetry

interface Telemetry {
    fun logEvent(
        name: String,
        params: Map<String, Any> = emptyMap(),
    )

    fun recordNonFatal(throwable: Throwable)

    fun setUserProperty(
        name: String,
        value: String,
    )
}

object TelemetryEvents {
    const val EXPENSE_CREATED = "expense_created"
    const val RECEIPT_CAPTURED = "receipt_captured"
    const val CSV_IMPORTED = "csv_imported"
    const val SYNC_COMPLETED = "sync_completed"
    const val INSIGHTS_VIEWED = "insights_viewed"
}
