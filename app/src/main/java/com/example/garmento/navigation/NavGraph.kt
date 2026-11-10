package com.example.garmento.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.garmento.feature.auth.LoginScreen
import com.example.garmento.feature.auth.SessionGateScreen
import com.example.garmento.feature.auth.StaffListScreen
import com.example.garmento.feature.customer_portal.CustomerPortalScreen
import com.example.garmento.feature.customers.CustomerDetailScreen
import com.example.garmento.feature.customers.CustomerListScreen
import com.example.garmento.feature.customers.NewCustomerScreen
import com.example.garmento.feature.customers.NewSubscriptionScreen
import com.example.garmento.feature.dashboard.ActiveSubscriptionsScreen
import com.example.garmento.feature.dashboard.DashboardScreen
import com.example.garmento.feature.dashboard.MainTab
import com.example.garmento.feature.orders.OrderEditScreen
import com.example.garmento.feature.orders.OrdersScreen
import com.example.garmento.feature.orders.PlacedOrdersScreen
import com.example.garmento.feature.plans.PlanEditScreen
import com.example.garmento.feature.plans.PlansListScreen
import com.example.garmento.feature.reminders.RemindersScreen
import com.example.garmento.feature.reports.ReportsScreen
import com.example.garmento.feature.settings.SettingsScreen
import com.example.garmento.feature.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.Session.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Session.route) {
            SessionGateScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Session.route) { inclusive = true }
                    }
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Session.route) { inclusive = true }
                    }
                }
            )
        }

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
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.createRoute())
                },
                onNavigateToPlans = {
                    navController.navigate(Screen.PlansList.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToActiveSubscriptions = {
                    navController.navigate(Screen.ActiveSubscriptions.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStaff = {
                    navController.navigate(Screen.Staff.route)
                },
                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
            )
        }

        composable(Screen.PlansList.route) {
            PlansListScreen(
                onNavigateToEdit = { planId ->
                    navController.navigate(Screen.PlanEdit.createRoute(planId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.PlanEdit.createRoute())
                },
                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
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
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.NewCustomer.route)
                },
                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
            )
        }

        composable(Screen.NewCustomer.route) {
            NewCustomerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CustomerEdit.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) {
            NewCustomerScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToEditCustomer = { customerId ->
                    navController.navigate(Screen.CustomerEdit.createRoute(customerId))
                },
                onNavigateToNewSubscription = { customerId ->
                    navController.navigate(Screen.NewSubscription.createRoute(customerId))
                },
                onNavigateToPlaceOrder = { customerId, subscriptionId ->
                    navController.navigate(Screen.Orders.createRoute(customerId, subscriptionId))
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
            route = Screen.Orders.route,
            arguments = listOf(
                navArgument("customerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("subscriptionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            OrdersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNewSubscription = { customerId ->
                    navController.navigate(Screen.NewSubscription.createRoute(customerId))
                },
                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
            )
        }

        composable(Screen.PlacedOrders.route) {
            PlacedOrdersScreen(
                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
            )
        }

        composable(Screen.ActiveSubscriptions.route) {
            ActiveSubscriptionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onRenew = { customerId ->
                    navController.navigate(Screen.NewSubscription.createRoute(customerId))
                }
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

private fun navigateToTab(navController: NavHostController, tab: MainTab) {
    val route = when (tab) {
        MainTab.DASHBOARD -> Screen.Dashboard.route
        MainTab.CUSTOMERS -> Screen.CustomerList.route
        MainTab.ORDERS -> Screen.Orders.createRoute()
        MainTab.PLACED_ORDERS -> Screen.PlacedOrders.route
        MainTab.PLANS -> Screen.PlansList.route
        MainTab.REPORTS -> Screen.Reports.route
    }
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        popUpTo(Screen.Dashboard.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
