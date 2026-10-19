package com.spendsnap.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsnap.data.repository.ExpenseRepository
import com.spendsnap.telemetry.Telemetry
import com.spendsnap.telemetry.TelemetryEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val selectedMonth: YearMonth = YearMonth.now(),
    val data: InsightsData? = null,
    val error: String? = null,
)

sealed interface InsightsEvent {
    data object PreviousMonth : InsightsEvent

    data object NextMonth : InsightsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        private val repository: ExpenseRepository,
        private val telemetry: Telemetry,
    ) : ViewModel() {
        private val selectedMonth = MutableStateFlow(YearMonth.now())
        private val error = MutableStateFlow<String?>(null)

        val uiState =
            selectedMonth
                .flatMapLatest { month ->
                    combine(
                        repository.observeExpenses(),
                        repository.observeCategories(),
                        repository.observeUnsyncedCount(),
                        error,
                    ) { expenses, categories, pending, currentError ->
                        InsightsUiState(
                            isLoading = false,
                            selectedMonth = month,
                            data =
                                InsightsCalculator.calculate(
                                    expenses = expenses,
                                    categories = categories,
                                    selectedMonth = month,
                                    pendingCount = pending,
                                ),
                            error = currentError,
                        )
                    }
                }
                .catch { throwable ->
                    error.value = throwable.message ?: "Could not load insights"
                    emit(
                        InsightsUiState(
                            isLoading = false,
                            selectedMonth = selectedMonth.value,
                            error = error.value,
                        ),
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = InsightsUiState(selectedMonth = selectedMonth.value),
                )

        init {
            logInsightsViewed(selectedMonth.value)
        }

        fun onEvent(event: InsightsEvent) {
            when (event) {
                InsightsEvent.PreviousMonth -> {
                    selectedMonth.update { it.minusMonths(1) }
                    logInsightsViewed(selectedMonth.value)
                }

                InsightsEvent.NextMonth -> {
                    selectedMonth.update { it.plusMonths(1) }
                    logInsightsViewed(selectedMonth.value)
                }
            }
        }

        private fun logInsightsViewed(month: YearMonth) {
            telemetry.logEvent(
                TelemetryEvents.INSIGHTS_VIEWED,
                mapOf("month" to month.toString()),
            )
        }
    }
