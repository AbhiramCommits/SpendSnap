package com.spendsnap.ui.navigation

sealed class Route(val route: String) {
    data object Expenses : Route("expenses")

    data object AddExpense : Route("expense/add?expenseId={expenseId}") {
        fun create(expenseId: String? = null): String =
            if (expenseId == null) "expense/add" else "expense/add?expenseId=$expenseId"
    }

    data object ExpenseDetail : Route("expense/{id}") {
        fun create(id: String): String = "expense/$id"
    }

    data object Insights : Route("insights")
}

val bottomDestinationRoutes = setOf(Route.Expenses.route, Route.Insights.route)
