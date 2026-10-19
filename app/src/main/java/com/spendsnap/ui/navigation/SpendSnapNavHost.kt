package com.spendsnap.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendsnap.feature.addexpense.AddExpenseScreen
import com.spendsnap.feature.detail.ExpenseDetailScreen
import com.spendsnap.feature.expenses.ExpenseListScreen
import com.spendsnap.feature.insights.InsightsScreen

@Composable
fun SpendSnapNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomDestinationRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected =
                            currentDestination?.hierarchy?.any {
                                it.route == Route.Expenses.route
                            } == true,
                        onClick = {
                            navController.navigate(Route.Expenses.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "Expenses tab"
                                    },
                            )
                        },
                        label = { Text("Expenses") },
                    )
                    NavigationBarItem(
                        selected =
                            currentDestination?.hierarchy?.any {
                                it.route == Route.Insights.route
                            } == true,
                        onClick = {
                            navController.navigate(Route.Insights.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Insights,
                                contentDescription = null,
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "Insights tab"
                                    },
                            )
                        },
                        label = { Text("Insights") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Expenses.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Route.Expenses.route) {
                ExpenseListScreen(
                    onAddExpense = { navController.navigate(Route.AddExpense.create()) },
                    onExpenseClick = { id ->
                        navController.navigate(Route.ExpenseDetail.create(id))
                    },
                )
            }
            composable(
                route = Route.AddExpense.route,
                arguments =
                    listOf(
                        navArgument("expenseId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
            ) { entry ->
                AddExpenseScreen(
                    expenseId = entry.arguments?.getString("expenseId"),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                route = Route.ExpenseDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ExpenseDetailScreen(
                    expenseId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Route.AddExpense.create(id)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(Route.Insights.route) {
                InsightsScreen()
            }
        }
    }
}
