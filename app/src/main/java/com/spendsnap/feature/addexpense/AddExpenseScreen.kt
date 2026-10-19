package com.spendsnap.feature.addexpense

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendsnap.core.ui.CategoryDot
import com.spendsnap.core.ui.ErrorBanner
import com.spendsnap.core.ui.LoadingState
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.ui.theme.SpendSnapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingReceiptUri by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success) {
                pendingReceiptUri?.let { viewModel.onEvent(AddExpenseEvent.ReceiptCaptured(it)) }
            }
        }
    val openCsv =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val content = withContext(Dispatchers.IO) { readText(context, uri) }
                    if (content != null) {
                        viewModel.onEvent(AddExpenseEvent.ImportCsv(content))
                    }
                }
            }
        }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    AddExpenseContent(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        onCaptureReceipt = {
            val uri = createReceiptUri(context)
            pendingReceiptUri = uri.toString()
            takePicture.launch(uri)
        },
        onImportCsv = {
            openCsv.launch(arrayOf("text/csv", "text/plain", "text/comma-separated-values"))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseContent(
    state: AddExpenseUiState,
    onBack: () -> Unit,
    onEvent: (AddExpenseEvent) -> Unit,
    onCaptureReceipt: () -> Unit,
    onImportCsv: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag("add_expense_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.expenseId == null) "New expense" else "Edit expense")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                label = "Loading expense form",
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    state.formError?.let { formError ->
                        ErrorBanner(
                            message = formError,
                            modifier = Modifier.testTag("form_error"),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.merchant,
                        onValueChange = {
                            onEvent(AddExpenseEvent.MerchantChanged(it))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("merchant_field")
                                .semantics {
                                    contentDescription = "Merchant name"
                                    state.merchantError?.let { error(it) }
                                },
                        label = { Text("Merchant") },
                        singleLine = true,
                        isError = state.merchantError != null,
                        supportingText = {
                            state.merchantError?.let {
                                Text(it, modifier = Modifier.testTag("merchant_error"))
                            }
                        },
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = {
                            onEvent(AddExpenseEvent.AmountChanged(it))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("amount_field")
                                .semantics {
                                    contentDescription = "Amount in dollars"
                                    state.amountError?.let { error(it) }
                                },
                        label = { Text("Amount") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.amountError != null,
                        supportingText = {
                            state.amountError?.let {
                                Text(it, modifier = Modifier.testTag("amount_error"))
                            }
                        },
                    )
                }
                item {
                    Text("Category", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = state.selectedCategoryId == category.id,
                                onClick = {
                                    onEvent(
                                        AddExpenseEvent.CategorySelected(category.id),
                                    )
                                },
                                modifier = Modifier.testTag("category_${category.id}"),
                                label = { Text(category.name) },
                                leadingIcon = {
                                    CategoryDot(category.colorHex)
                                },
                            )
                        }
                    }
                }
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .testTag("date_picker_button")
                                .semantics { contentDescription = "Expense date" },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Expense date")
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Date", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    Instant.ofEpochMilli(state.occurredAt)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .format(
                                            DateTimeFormatter.ofPattern(
                                                "MMMM d, yyyy",
                                                Locale.getDefault(),
                                            ),
                                        ),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { onEvent(AddExpenseEvent.NoteChanged(it)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("note_field"),
                        label = { Text("Note (optional)") },
                        minLines = 2,
                        maxLines = 4,
                    )
                }
                item {
                    Text("Receipt", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onCaptureReceipt,
                            modifier = Modifier.testTag("capture_receipt_button"),
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = "Capture receipt",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Camera")
                        }
                        OutlinedButton(
                            onClick = onImportCsv,
                            modifier = Modifier.testTag("import_csv_button"),
                        ) {
                            Icon(
                                Icons.Outlined.UploadFile,
                                contentDescription = "Import CSV",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Import CSV")
                        }
                    }
                    if (state.receiptUri != null) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = "Receipt attached",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Receipt image attached",
                                modifier = Modifier.testTag("receipt_attached"),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                val importSummary = state.importSummary
                if (importSummary != null) {
                    item {
                        CsvReport(
                            summary = importSummary,
                            errors = state.importErrors.map { "Line ${it.lineNumber}: ${it.message}" },
                            onDismiss = {
                                onEvent(AddExpenseEvent.DismissImportReport)
                            },
                        )
                    }
                }
                item {
                    Button(
                        onClick = { onEvent(AddExpenseEvent.Save) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("save_expense_button"),
                        enabled = !state.isSaving && !state.isImporting,
                    ) {
                        Text(if (state.isSaving) "Saving..." else "Save expense")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = pickerState.selectedDateMillis
                        if (selected != null) {
                            val localDate =
                                Instant.ofEpochMilli(selected)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            onEvent(
                                AddExpenseEvent.DateSelected(
                                    localDate.atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli(),
                                ),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CsvReport(
    summary: String,
    errors: List<String>,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(summary, style = MaterialTheme.typography.titleMedium)
            errors.take(5).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (errors.size > 5) {
                Text("and ${errors.size - 5} more", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

private fun createReceiptUri(context: Context): Uri {
    val directory = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(directory, "receipt-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

private fun readText(
    context: Context,
    uri: Uri,
): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

@Preview(name = "Add expense light", showBackground = true)
@Composable
fun AddExpenseLightPreview() {
    SpendSnapTheme(darkTheme = false) {
        AddExpenseContent(
            state = previewAddExpenseState(),
            onBack = {},
            onEvent = {},
            onCaptureReceipt = {},
            onImportCsv = {},
        )
    }
}

@Preview(name = "Add expense dark", showBackground = true)
@Composable
fun AddExpenseDarkPreview() {
    SpendSnapTheme(darkTheme = true) {
        AddExpenseContent(
            state = previewAddExpenseState(),
            onBack = {},
            onEvent = {},
            onCaptureReceipt = {},
            onImportCsv = {},
        )
    }
}

private fun previewAddExpenseState() =
    AddExpenseUiState(
        isLoading = false,
        merchant = "Corner Cafe",
        amount = "12.50",
        categories = DatabaseSeeder.DEFAULT_CATEGORIES,
        selectedCategoryId = "category-meals",
        note = "Coffee with the design team",
    )
