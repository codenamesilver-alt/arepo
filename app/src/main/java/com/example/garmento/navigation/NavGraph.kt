package com.example.garmento.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            PlaceholderLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            PlaceholderDashboardScreen(
                onNavigateToReminders = {
                    navController.navigate(Screen.Reminders.route)
                },
                onNavigateToCustomers = {
                    navController.navigate(Screen.CustomerList.route)
                }
            )
        }

        composable(Screen.PlansList.route) {
            PlaceholderPlansListScreen(
                onNavigateToEdit = { planId ->
                    navController.navigate(Screen.PlanEdit.createRoute(planId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.PlanEdit.createRoute())
                }
            )
        }

        composable(
            route = Screen.PlanEdit.route,
            arguments = listOf(
                navArgument("planId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId")
            PlaceholderPlanEditScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerList.route) {
            PlaceholderCustomerListScreen(
                onNavigateToDetail = { customerId ->
                    navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                }
            )
        }

        composable(
            route = Screen.CustomerDetail.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) {
            PlaceholderCustomerDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNewSubscription = { customerId ->
                    navController.navigate(Screen.NewSubscription.createRoute(customerId))
                },
                onNavigateToPlaceOrder = { customerId ->
                    navController.navigate(Screen.PlaceOrder.createRoute(customerId))
                },
                onNavigateToOrderEdit = { orderId ->
                    navController.navigate(Screen.OrderEdit.createRoute(orderId))
                }
            )
        }

        composable(
            route = Screen.NewSubscription.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) {
            PlaceholderNewSubscriptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlaceOrder.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) {
            PlaceholderPlaceOrderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.OrderEdit.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) {
            PlaceholderOrderEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            PlaceholderReportsScreen()
        }

        composable(Screen.Reminders.route) {
            PlaceholderRemindersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            PlaceholderSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Placeholders below replace feature screens in Tasks 8+. Signatures match the
// contracts the real screens must expose, so swapping them in is a drop-in change.

@Composable
private fun PlaceholderLoginScreen(onLoginSuccess: () -> Unit) {
    PlaceholderContent("Login")
}

@Composable
private fun PlaceholderDashboardScreen(
    onNavigateToReminders: () -> Unit,
    onNavigateToCustomers: () -> Unit
) {
    PlaceholderContent("Dashboard")
}

@Composable
private fun PlaceholderPlansListScreen(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    PlaceholderContent("Plans")
}

@Composable
private fun PlaceholderPlanEditScreen(
    planId: String?,
    onNavigateBack: () -> Unit
) {
    PlaceholderContent("Plan Edit")
}

@Composable
private fun PlaceholderCustomerListScreen(onNavigateToDetail: (String) -> Unit) {
    PlaceholderContent("Customers")
}

@Composable
private fun PlaceholderCustomerDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNewSubscription: (String) -> Unit,
    onNavigateToPlaceOrder: (String) -> Unit,
    onNavigateToOrderEdit: (String) -> Unit
) {
    PlaceholderContent("Customer Detail")
}

@Composable
private fun PlaceholderNewSubscriptionScreen(onNavigateBack: () -> Unit) {
    PlaceholderContent("New Subscription")
}

@Composable
private fun PlaceholderPlaceOrderScreen(onNavigateBack: () -> Unit) {
    PlaceholderContent("Place Order")
}

@Composable
private fun PlaceholderOrderEditScreen(onNavigateBack: () -> Unit) {
    PlaceholderContent("Order Edit")
}

@Composable
private fun PlaceholderReportsScreen() {
    PlaceholderContent("Reports")
}

@Composable
private fun PlaceholderRemindersScreen(onNavigateBack: () -> Unit) {
    PlaceholderContent("Reminders")
}

@Composable
private fun PlaceholderSettingsScreen(onNavigateBack: () -> Unit) {
    PlaceholderContent("Settings")
}

@Composable
private fun PlaceholderContent(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title — coming soon")
    }
}