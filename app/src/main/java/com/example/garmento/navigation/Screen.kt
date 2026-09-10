package com.example.garmento.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object PlansList : Screen("plans")
    data object PlanEdit : Screen("plan_edit?planId={planId}") {
        fun createRoute(planId: String? = null): String =
            if (planId != null) "plan_edit?planId=$planId" else "plan_edit"
    }
    data object CustomerList : Screen("customers")
    data object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String): String = "customer_detail/$customerId"
    }
    data object NewSubscription : Screen("new_subscription/{customerId}") {
        fun createRoute(customerId: String): String = "new_subscription/$customerId"
    }
    data object PlaceOrder : Screen("place_order/{customerId}") {
        fun createRoute(customerId: String): String = "place_order/$customerId"
    }
    data object OrderEdit : Screen("order_edit/{orderId}") {
        fun createRoute(orderId: String): String = "order_edit/$orderId"
    }
    data object Reports : Screen("reports")
    data object Reminders : Screen("reminders")
    data object Settings : Screen("settings")
}