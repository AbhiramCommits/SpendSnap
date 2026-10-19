package com.spendsnap.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendsnap.data.local.SyncState
import java.util.UUID

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["syncState"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val merchant: String,
    val amountCents: Long,
    val currency: String = "USD",
    val categoryId: String,
    val occurredAt: Long,
    val note: String = "",
    val receiptUri: String? = null,
    val syncState: SyncState = SyncState.PENDING,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
