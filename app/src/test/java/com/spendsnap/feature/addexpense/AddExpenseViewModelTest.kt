package com.spendsnap.feature.addexpense

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.spendsnap.core.csv.CsvExpenseImporter
import com.spendsnap.data.local.SyncState
import com.spendsnap.feature.expenses.awaitMatching
import com.spendsnap.testutil.FakeExpenseRepository
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeExpenseRepository
    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        Dispatchers.setMain(dispatcher)
        repository = FakeExpenseRepository()
        viewModel = AddExpenseViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(),
            csvImporter = CsvExpenseImporter(),
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        Dispatchers.resetMain()
    }

    @Test
    fun save_withInvalidAmount_showsValidationAndDoesNotWrite() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitMatching { !it.isLoading }
            viewModel.onEvent(AddExpenseEvent.MerchantChanged("Corner Cafe"))
            viewModel.onEvent(AddExpenseEvent.AmountChanged("abc"))
            viewModel.onEvent(AddExpenseEvent.Save)

            val invalid = awaitMatching { it.amountError != null }
            assertThat(invalid.amountError).isEqualTo("Enter an amount greater than zero")
            assertThat(invalid.saved).isFalse()
            assertThat(repository.upserted).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun save_withValidForm_writesPendingExpenseWithParsedCents() = runTest(dispatcher) {
        viewModel.uiState.test {
            val loaded = awaitMatching { !it.isLoading }
            viewModel.onEvent(AddExpenseEvent.MerchantChanged("  Corner Cafe "))
            viewModel.onEvent(AddExpenseEvent.AmountChanged("12.5"))
            viewModel.onEvent(AddExpenseEvent.CategorySelected(loaded.categories.first().id))
            viewModel.onEvent(AddExpenseEvent.NoteChanged("Team breakfast"))
            viewModel.onEvent(AddExpenseEvent.ReceiptCaptured("file:///receipt.jpg"))
            viewModel.onEvent(AddExpenseEvent.Save)

            val saved = awaitMatching { it.saved }
            val entity = repository.upserted.single()
            assertThat(saved.isSaving).isFalse()
            assertThat(entity.merchant).isEqualTo("Corner Cafe")
            assertThat(entity.amountCents).isEqualTo(1_250)
            assertThat(entity.note).isEqualTo("Team breakfast")
            assertThat(entity.receiptUri).isEqualTo("file:///receipt.jpg")
            assertThat(entity.syncState).isEqualTo(SyncState.PENDING)
            assertThat(entity.categoryId).isEqualTo("category-meals")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun csvImport_bulkInsertsValidRowsAndReportsRowErrors() = runTest(dispatcher) {
        val csv = """
            date,merchant,amount,category
            2026-10-01,Cafe,10.00,Meals
            2026-10-02,Broken,abc,Meals
        """.trimIndent()

        viewModel.uiState.test {
            awaitMatching { !it.isLoading }
            viewModel.onEvent(AddExpenseEvent.ImportCsv(csv))

            val imported = awaitMatching { it.importSummary != null }
            assertThat(imported.importSummary).isEqualTo("1 imported, 1 row error(s)")
            assertThat(imported.importErrors).hasSize(1)
            assertThat(repository.upserted).hasSize(1)
            assertThat(repository.upserted.single().amountCents).isEqualTo(1_000)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
