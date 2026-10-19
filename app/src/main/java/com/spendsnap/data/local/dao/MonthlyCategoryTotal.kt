package com.spendsnap.data.local.dao

data class MonthlyCategoryTotal(
    val categoryId: String,
    val totalCents: Long,
    val expenseCount: Int,
)
