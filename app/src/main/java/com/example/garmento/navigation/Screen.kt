package com.example.garmento.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Session : Screen("session")
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object PlacedOrders : Screen("placed_orders")
    data object PlansList : Screen("plans")
    data object PlanEdit : Screen("plan_edit?planId={planId}") {
        fun createRoute(planId: String? = null): String =
            if (planId != null) "plan_edit?planId=$planId" else "plan_edit"
    }
    data object NewCustomer : Screen("new_customer")
    data object CustomerEdit : Screen("customer_edit/{customerId}") {
        fun createRoute(customerId: String): String = "customer_edit/$customerId"
    }
    data object CustomerList : Screen("customers")
    data object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String): String = "customer_detail/$customerId"
    }
    data object NewSubscription : Screen("new_subscription/{customerId}") {
        fun createRoute(customerId: String): String = "new_subscription/$customerId"
    }
    data object Orders : Screen("orders?customerId={customerId}&subscriptionId={subscriptionId}") {
        fun createRoute(customerId: String? = null, subscriptionId: String? = null): String {
            val params = buildList {
                if (customerId != null) add("customerId=$customerId")
                if (subscriptionId != null) add("subscriptionId=$subscriptionId")
            }
            return if (params.isEmpty()) "orders" else "orders?${params.joinToString("&")}"
        }
    }
    data object OrderEdit : Screen("order_edit/{orderId}") {
        fun createRoute(orderId: String): String = "order_edit/$orderId"
    }
    data object Reports : Screen("reports")
    data object ActiveSubscriptions : Screen("active_subscriptions")
    data object Reminders : Screen("reminders")
    data object Staff : Screen("staff")
    data object Settings : Screen("settings")
    data object CustomerPortal : Screen("customer_portal/{publicCode}") {
        fun createRoute(publicCode: String): String = "customer_portal/$publicCode"
    }
}