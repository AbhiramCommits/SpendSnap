package com.spendsnap.feature.detail

import com.spendsnap.data.local.SyncState

data class ExpenseDetailUi(
    val id: String,
    val merchant: String,
    val amountCents: Long,
    val currency: String,
    val categoryName: String,
    val categoryColorHex: String,
    val occurredAt: Long,
    val note: String,
    val receiptUri: String?,
    val syncState: SyncState,
)

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val expense: ExpenseDetailUi? = null,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
)
