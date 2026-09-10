package com.example.garmento.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.garmento.feature.auth.LoginScreen
import com.example.garmento.feature.auth.StaffListScreen
import com.example.garmento.feature.customer_portal.CustomerPortalScreen
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
import com.example.garmento.feature.settings.SettingsScreen

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

        composable(Screen.Staff.route) {
            StaffListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStaff = {
                    navController.navigate(Screen.Staff.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.CustomerPortal.route,
            arguments = listOf(
                navArgument("publicCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val publicCode = backStackEntry.arguments?.getString("publicCode") ?: ""
            CustomerPortalScreen(publicCode = publicCode)
        }
    }
}
