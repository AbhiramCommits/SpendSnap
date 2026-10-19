package com.spendsnap.feature.detail

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendsnap.core.ui.CategoryDot
import com.spendsnap.core.ui.EmptyState
import com.spendsnap.core.ui.ErrorBanner
import com.spendsnap.core.ui.LoadingState
import com.spendsnap.core.ui.MoneyText
import com.spendsnap.core.ui.SyncBadge
import com.spendsnap.data.local.SyncState
import com.spendsnap.ui.theme.SpendSnapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    ExpenseDetailContent(
        expenseId = expenseId,
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailContent(
    expenseId: String,
    state: ExpenseDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("expense_detail_screen_$expenseId"),
        topBar = {
            TopAppBar(
                title = { Text("Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.expense != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_expense_button"),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit expense")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("delete_expense_button"),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete expense")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading ->
                LoadingState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    label = "Loading expense",
                )

            state.expense == null ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    state.error?.let { error ->
                        ErrorBanner(
                            message = error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    EmptyState(
                        modifier = Modifier.weight(1f),
                        title = "Expense not found",
                        message = "It may have been deleted on this device.",
                    )
                }

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.error?.let { error ->
                        ErrorBanner(message = error)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                state.expense?.merchant.orEmpty(),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            MoneyText(
                                amountCents = state.expense?.amountCents ?: 0,
                                currency = state.expense?.currency ?: "USD",
                                modifier = Modifier.testTag("detail_amount"),
                                style = MaterialTheme.typography.displaySmall,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryDot(state.expense?.categoryColorHex ?: "#6C757D")
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.expense?.categoryName.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(Modifier.width(12.dp))
                                SyncBadge(
                                    state = state.expense?.syncState ?: SyncState.PENDING,
                                    modifier = Modifier.testTag("detail_sync_badge"),
                                )
                            }
                            Text(
                                Instant.ofEpochMilli(state.expense?.occurredAt ?: 0)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(
                                        DateTimeFormatter.ofPattern(
                                            "MMMM d, yyyy",
                                            Locale.getDefault(),
                                        ),
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!state.expense?.note.isNullOrBlank()) {
                                Text(
                                    state.expense?.note.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }

                    Text("Receipt", style = MaterialTheme.typography.titleMedium)
                    val receiptUri = state.expense?.receiptUri
                    if (receiptUri == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = "No receipt attached",
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("No receipt attached")
                            }
                        }
                    } else {
                        ReceiptThumbnail(uri = receiptUri)
                    }

                    if (state.isDeleting) {
                        Text("Deleting...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete expense?") },
            text = { Text("This removes the local record. Synced records stay in the demo backend.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ReceiptThumbnail(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = uri,
    ) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                }.getOrNull()
            }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = "Receipt image",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("receipt_thumbnail"),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                "Receipt image could not be loaded",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(name = "Expense detail light", showBackground = true)
@Composable
fun ExpenseDetailLightPreview() {
    SpendSnapTheme(darkTheme = false) {
        ExpenseDetailContent(
            expenseId = "preview",
            state = previewDetailState(),
            onBack = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Expense detail dark", showBackground = true)
@Composable
fun ExpenseDetailDarkPreview() {
    SpendSnapTheme(darkTheme = true) {
        ExpenseDetailContent(
            expenseId = "preview",
            state = previewDetailState(),
            onBack = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

private fun previewDetailState() =
    ExpenseDetailUiState(
        isLoading = false,
        expense =
            ExpenseDetailUi(
                id = "preview",
                merchant = "Northwind Books",
                amountCents = 4_250,
                currency = "USD",
                categoryName = "Office",
                categoryColorHex = "#A06A2C",
                occurredAt = System.currentTimeMillis(),
                note = "Reference book for the team",
                receiptUri = null,
                syncState = SyncState.PENDING,
            ),
    )
