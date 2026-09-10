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
import com.example.garmento.feature.auth.LoginScreen
import com.example.garmento.feature.customers.CustomerDetailScreen
import com.example.garmento.feature.customers.CustomerListScreen
import com.example.garmento.feature.customers.NewSubscriptionScreen
import com.example.garmento.feature.dashboard.DashboardScreen
import com.example.garmento.feature.orders.OrderEditScreen
import com.example.garmento.feature.orders.PlaceOrderScreen
import com.example.garmento.feature.plans.PlanEditScreen
import com.example.garmento.feature.plans.PlansListScreen
import com.example.garmento.feature.reminders.RemindersScreen
import com.example.garmento.feature.reports.ReportsScreen

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
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToReminders = {
                    navController.navigate(Screen.Reminders.route)
                },
                onNavigateToCustomers = {
                    navController.navigate(Screen.CustomerList.route)
                }
            )
        }

        composable(Screen.PlansList.route) {
            PlansListScreen(
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
            PlanEditScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerList.route) {
            CustomerListScreen(
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
            CustomerDetailScreen(
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
            NewSubscriptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlaceOrder.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) {
            PlaceOrderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.OrderEdit.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) {
            OrderEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reminders.route) {
            RemindersScreen(
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