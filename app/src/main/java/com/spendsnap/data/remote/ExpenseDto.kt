package com.spendsnap.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDto(
    val clientUuid: String,
    val merchant: String,
    val amountCents: Long,
    val currency: String,
    val categoryId: String,
    val occurredAt: Long,
    val note: String = "",
    val receiptUri: String? = null,
    val updatedAt: Long,
    val remoteId: String? = null,
)

@Serializable
data class ExpenseListResponse(
    val expenses: List<ExpenseDto> = emptyList(),
)
