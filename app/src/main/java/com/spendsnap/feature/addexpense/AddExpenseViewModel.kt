package com.spendsnap.feature.addexpense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsnap.core.amount.AmountParser
import com.spendsnap.core.csv.CsvExpenseImporter
import com.spendsnap.data.local.entity.ExpenseEntity
import com.spendsnap.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    savedStateHandle: SavedStateHandle,
    private val csvImporter: CsvExpenseImporter,
) : ViewModel() {
    private val initialExpenseId: String? = savedStateHandle["expenseId"]
    private val _uiState = MutableStateFlow(
        AddExpenseUiState(expenseId = initialExpenseId),
    )
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: AddExpenseEvent) {
        when (event) {
            is AddExpenseEvent.MerchantChanged -> _uiState.update {
                it.copy(merchant = event.value, merchantError = null, formError = null)
            }

            is AddExpenseEvent.AmountChanged -> _uiState.update {
                it.copy(amount = event.value, amountError = null, formError = null)
            }

            is AddExpenseEvent.CategorySelected -> _uiState.update {
                it.copy(selectedCategoryId = event.categoryId)
            }

            is AddExpenseEvent.DateSelected -> _uiState.update {
                it.copy(occurredAt = event.occurredAt)
            }

            is AddExpenseEvent.NoteChanged -> _uiState.update {
                it.copy(note = event.value)
            }

            is AddExpenseEvent.ReceiptCaptured -> _uiState.update {
                it.copy(receiptUri = event.uri)
            }

            is AddExpenseEvent.ImportCsv -> importCsv(event.content)
            AddExpenseEvent.Save -> save()
            AddExpenseEvent.DismissError -> _uiState.update { it.copy(formError = null) }
            AddExpenseEvent.DismissImportReport -> _uiState.update {
                it.copy(importSummary = null, importErrors = emptyList())
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val categories = repository.observeCategories().first()
            val existing = initialExpenseId?.let { repository.observeExpense(it).first() }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    categories = categories,
                    selectedCategoryId = existing?.categoryId
                        ?: state.selectedCategoryId.ifBlank { categories.firstOrNull()?.id.orEmpty() },
                    merchant = existing?.merchant ?: state.merchant,
                    amount = existing?.let { formatCentsForEditing(it.amountCents) } ?: state.amount,
                    occurredAt = existing?.occurredAt ?: state.occurredAt,
                    note = existing?.note ?: state.note,
                    receiptUri = existing?.receiptUri ?: state.receiptUri,
                    formError = if (initialExpenseId != null && existing == null) {
                        "Expense not found"
                    } else {
                        state.formError
                    },
                )
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val merchant = state.merchant.trim()
        val merchantError = if (merchant.isEmpty()) "Merchant is required" else null
        val amountCents = AmountParser.parse(state.amount, Locale.getDefault())
        val amountError = if (amountCents == null) {
            "Enter an amount greater than zero"
        } else {
            null
        }

        if (merchantError != null || amountError != null) {
            _uiState.update {
                it.copy(merchantError = merchantError, amountError = amountError)
            }
            return
        }
        if (state.selectedCategoryId.isBlank()) {
            _uiState.update { it.copy(formError = "Choose a category") }
            return
        }

        val cents = requireNotNull(amountCents)
        _uiState.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            try {
                repository.upsert(
                    ExpenseEntity(
                        id = state.expenseId ?: UUID.randomUUID().toString(),
                        merchant = merchant,
                        amountCents = cents,
                        categoryId = state.selectedCategoryId,
                        occurredAt = state.occurredAt,
                        note = state.note.trim(),
                        receiptUri = state.receiptUri,
                    ),
                )
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        formError = error.message ?: "Could not save this expense",
                    )
                }
            }
        }
    }

    private fun importCsv(content: String) {
        val categories = _uiState.value.categories
        if (categories.isEmpty()) {
            _uiState.update { it.copy(formError = "Categories are still loading") }
            return
        }
        _uiState.update { it.copy(isImporting = true, formError = null) }
        viewModelScope.launch {
            val result = csvImporter.parse(content, categories)
            try {
                if (result.expenses.isNotEmpty()) {
                    repository.upsertAll(result.expenses)
                }
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importSummary = result.summary,
                        importErrors = result.errors,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        formError = error.message ?: "CSV import failed",
                    )
                }
            }
        }
    }

    private fun formatCentsForEditing(cents: Long): String =
        BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()
}
