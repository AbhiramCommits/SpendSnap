package com.spendsnap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.spendsnap.data.local.SpendSnapDatabase
import com.spendsnap.di.DatabaseModule
import com.spendsnap.di.NetworkModule
import com.spendsnap.di.RepositoryModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DatabaseModule::class, NetworkModule::class, RepositoryModule::class)
class AddExpenseScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: SpendSnapDatabase

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun addingExpense_showsItWithPendingBadge() {
        composeRule.waitForTag("expense_list_screen")
        composeRule.onNodeWithTag("add_expense_fab").performClick()
        composeRule.waitForTag("add_expense_screen")

        composeRule.onNodeWithTag("merchant_field").performTextInput("Corner Cafe")
        composeRule.onNodeWithTag("amount_field").performTextInput("12.50")
        composeRule.onNodeWithTag("save_expense_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onAllNodesWithText("Corner Cafe").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Corner Cafe").assertIsDisplayed()
        composeRule.onNodeWithText("Pending").assertIsDisplayed()
    }

    @Test
    fun invalidAmount_showsValidationMessage() {
        composeRule.waitForTag("expense_list_screen")
        composeRule.onNodeWithTag("add_expense_fab").performClick()
        composeRule.waitForTag("add_expense_screen")

        composeRule.onNodeWithTag("merchant_field").performTextInput("Corner Cafe")
        composeRule.onNodeWithTag("amount_field").performTextInput("abc")
        composeRule.onNodeWithTag("save_expense_button").performClick()

        composeRule.onNodeWithText("Enter an amount greater than zero").assertIsDisplayed()
    }

    private fun AndroidComposeTestRule<*, *>.waitForTag(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            runCatching {
                onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }
}
