package com.spendsnap.feature.addexpense

import com.spendsnap.core.csv.CsvImportError
import com.spendsnap.data.local.entity.CategoryEntity

data class AddExpenseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val expenseId: String? = null,
    val merchant: String = "",
    val amount: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptUri: String? = null,
    val merchantError: String? = null,
    val amountError: String? = null,
    val formError: String? = null,
    val isImporting: Boolean = false,
    val importSummary: String? = null,
    val importErrors: List<CsvImportError> = emptyList(),
    val saved: Boolean = false,
)

sealed interface AddExpenseEvent {
    data class MerchantChanged(val value: String) : AddExpenseEvent

    data class AmountChanged(val value: String) : AddExpenseEvent

    data class CategorySelected(val categoryId: String) : AddExpenseEvent

    data class DateSelected(val occurredAt: Long) : AddExpenseEvent

    data class NoteChanged(val value: String) : AddExpenseEvent

    data class ReceiptCaptured(val uri: String) : AddExpenseEvent

    data class ImportCsv(val content: String) : AddExpenseEvent

    data object Save : AddExpenseEvent

    data object DismissError : AddExpenseEvent

    data object DismissImportReport : AddExpenseEvent
}
