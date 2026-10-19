package com.spendsnap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class ExpenseListScreenTest {
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
    fun emptyState_isVisibleOnFirstLaunch() {
        composeRule.waitForTag("expense_empty_state")

        composeRule.onNodeWithTag("expense_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("No expenses yet").assertIsDisplayed()
        composeRule.onNodeWithTag("monthly_total").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitForTag(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            runCatching {
                onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }
}
