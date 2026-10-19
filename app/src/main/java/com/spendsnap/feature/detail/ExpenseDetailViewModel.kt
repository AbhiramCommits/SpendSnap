package com.spendsnap.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsnap.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class DetailOperationState(
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val expenseId: String = savedStateHandle.get<String>("id").orEmpty()
    private val operation = MutableStateFlow(DetailOperationState())

    val uiState = combine(
        repository.observeExpense(expenseId),
        repository.observeCategories(),
        operation,
    ) { expense, categories, operationState ->
        val category = categories.firstOrNull { it.id == expense?.categoryId }
        ExpenseDetailUiState(
            isLoading = false,
            expense = expense?.let {
                ExpenseDetailUi(
                    id = it.id,
                    merchant = it.merchant,
                    amountCents = it.amountCents,
                    currency = it.currency,
                    categoryName = category?.name ?: "Other",
                    categoryColorHex = category?.colorHex ?: "#6C757D",
                    occurredAt = it.occurredAt,
                    note = it.note,
                    receiptUri = it.receiptUri,
                    syncState = it.syncState,
                )
            },
            isDeleting = operationState.isDeleting,
            deleted = operationState.deleted,
            error = operationState.error,
        )
    }
        .catch { throwable ->
            emit(
                ExpenseDetailUiState(
                    isLoading = false,
                    error = throwable.message ?: "Could not load this expense",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseDetailUiState(),
        )

    fun delete() {
        val currentExpense = uiState.value.expense ?: return
        operation.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            try {
                repository.delete(currentExpense.id)
                operation.update { it.copy(isDeleting = false, deleted = true) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                operation.update {
                    it.copy(
                        isDeleting = false,
                        error = error.message ?: "Could not delete this expense",
                    )
                }
            }
        }
    }

    fun dismissError() {
        operation.update { it.copy(error = null) }
    }
}
