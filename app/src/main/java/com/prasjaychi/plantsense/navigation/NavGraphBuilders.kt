package com.prasjaychi.plantsense.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.prasjaychi.plantsense.ui.account.AccountEditScreen
import com.prasjaychi.plantsense.ui.account.AccountProfileScreen
import com.prasjaychi.plantsense.ui.account.AccountSecurityScreen
import com.prasjaychi.plantsense.ui.account.AccountTeamScreen
import com.prasjaychi.plantsense.ui.order.OrderAddonsScreen
import com.prasjaychi.plantsense.ui.order.OrderConfirmationScreen
import com.prasjaychi.plantsense.ui.order.OrderPlanScreen
import com.prasjaychi.plantsense.ui.order.OrderReviewScreen
import com.prasjaychi.plantsense.viewmodel.AccountSharedViewModel
import com.prasjaychi.plantsense.viewmodel.NavInspectorViewModel
import com.prasjaychi.plantsense.viewmodel.OrderSharedViewModel

/**
 * Builds the nested Order Flow navigation graph.
 *
 * Scopes [OrderSharedViewModel] to [NavRoutes.ORDER_GRAPH] so all 4 sub-steps
 * share consistent draft order state.
 */
fun NavGraphBuilder.orderGraph(
    navController: NavController,
    inspectorViewModel: NavInspectorViewModel
) {
    navigation(
        startDestination = NavRoutes.ORDER_PLAN,
        route = NavRoutes.ORDER_GRAPH
    ) {
        // Step 1: Plan Selection
        composable(NavRoutes.ORDER_PLAN) { backStackEntry ->
            val orderViewModel: OrderSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ORDER_GRAPH
            )

            OrderPlanScreen(
                viewModel = orderViewModel,
                onNextStep = {
                    inspectorViewModel.logNavigation(
                        actionType = "navigate",
                        destination = NavRoutes.ORDER_ADDONS,
                        parentGraph = NavRoutes.ORDER_GRAPH,
                        details = "Step 1 -> Step 2"
                    )
                    navController.navigate(NavRoutes.ORDER_ADDONS)
                },
                onCancelFlow = {
                    inspectorViewModel.logNavigation(
                        actionType = "popUpTo",
                        destination = NavRoutes.DASHBOARD,
                        parentGraph = null,
                        details = "Canceled Order Flow"
                    )
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.ORDER_GRAPH) { inclusive = true }
                    }
                }
            )
        }

        // Step 2: Add-ons Configuration
        composable(NavRoutes.ORDER_ADDONS) { backStackEntry ->
            val orderViewModel: OrderSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ORDER_GRAPH
            )

            OrderAddonsScreen(
                viewModel = orderViewModel,
                onNextStep = {
                    inspectorViewModel.logNavigation(
                        actionType = "navigate",
                        destination = NavRoutes.ORDER_REVIEW,
                        parentGraph = NavRoutes.ORDER_GRAPH,
                        details = "Step 2 -> Step 3"
                    )
                    navController.navigate(NavRoutes.ORDER_REVIEW)
                },
                onPreviousStep = {
                    inspectorViewModel.logNavigation(
                        actionType = "popBackStack",
                        destination = NavRoutes.ORDER_PLAN,
                        parentGraph = NavRoutes.ORDER_GRAPH,
                        details = "Step 2 -> Step 1"
                    )
                    navController.popBackStack()
                }
            )
        }

        // Step 3: Review & Finalize
        composable(NavRoutes.ORDER_REVIEW) { backStackEntry ->
            val orderViewModel: OrderSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ORDER_GRAPH
            )

            OrderReviewScreen(
                viewModel = orderViewModel,
                onSubmitOrder = {
                    inspectorViewModel.logNavigation(
                        actionType = "navigate",
                        destination = NavRoutes.ORDER_CONFIRMATION,
                        parentGraph = NavRoutes.ORDER_GRAPH,
                        details = "Step 3 -> Step 4 (Order submitted)"
                    )
                    navController.navigate(NavRoutes.ORDER_CONFIRMATION)
                },
                onPreviousStep = {
                    inspectorViewModel.logNavigation(
                        actionType = "popBackStack",
                        destination = NavRoutes.ORDER_ADDONS,
                        parentGraph = NavRoutes.ORDER_GRAPH,
                        details = "Step 3 -> Step 2"
                    )
                    navController.popBackStack()
                }
            )
        }

        // Step 4: Confirmation & Completion
        composable(NavRoutes.ORDER_CONFIRMATION) { backStackEntry ->
            val orderViewModel: OrderSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ORDER_GRAPH
            )

            OrderConfirmationScreen(
                viewModel = orderViewModel,
                onCompleteAndReturnToDashboard = {
                    inspectorViewModel.logNavigation(
                        actionType = "popUpTo",
                        destination = NavRoutes.DASHBOARD,
                        parentGraph = null,
                        details = "popUpTo(ORDER_GRAPH) { inclusive = true }"
                    )
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.ORDER_GRAPH) {
                            inclusive = true
                        }
                    }
                },
                onStartNewOrder = {
                    inspectorViewModel.logNavigation(
                        actionType = "popUpTo",
                        destination = NavRoutes.ORDER_PLAN,
                        parentGraph = NavRoutes.ORDER_GRAPH,
                        details = "Restart order flow"
                    )
                    navController.navigate(NavRoutes.ORDER_PLAN) {
                        popUpTo(NavRoutes.ORDER_PLAN) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Builds the nested Account Settings navigation graph.
 *
 * Scopes [AccountSharedViewModel] to [NavRoutes.ACCOUNT_GRAPH] so Profile, Edit,
 * Security, and Team screens all share and mutate the same state.
 */
fun NavGraphBuilder.accountGraph(
    navController: NavController,
    inspectorViewModel: NavInspectorViewModel
) {
    navigation(
        startDestination = NavRoutes.ACCOUNT_PROFILE,
        route = NavRoutes.ACCOUNT_GRAPH
    ) {
        // Main Profile Overview
        composable(NavRoutes.ACCOUNT_PROFILE) { backStackEntry ->
            val accountViewModel: AccountSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ACCOUNT_GRAPH
            )

            AccountProfileScreen(
                viewModel = accountViewModel,
                onNavigateToEdit = {
                    inspectorViewModel.logNavigation("navigate", NavRoutes.ACCOUNT_EDIT, NavRoutes.ACCOUNT_GRAPH)
                    navController.navigate(NavRoutes.ACCOUNT_EDIT)
                },
                onNavigateToSecurity = {
                    inspectorViewModel.logNavigation("navigate", NavRoutes.ACCOUNT_SECURITY, NavRoutes.ACCOUNT_GRAPH)
                    navController.navigate(NavRoutes.ACCOUNT_SECURITY)
                },
                onNavigateToTeam = {
                    inspectorViewModel.logNavigation("navigate", NavRoutes.ACCOUNT_TEAM, NavRoutes.ACCOUNT_GRAPH)
                    navController.navigate(NavRoutes.ACCOUNT_TEAM)
                }
            )
        }

        // Edit Profile
        composable(NavRoutes.ACCOUNT_EDIT) { backStackEntry ->
            val accountViewModel: AccountSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ACCOUNT_GRAPH
            )

            AccountEditScreen(
                viewModel = accountViewModel,
                onNavigateBack = {
                    inspectorViewModel.logNavigation("popBackStack", NavRoutes.ACCOUNT_PROFILE, NavRoutes.ACCOUNT_GRAPH)
                    navController.popBackStack()
                }
            )
        }

        // Security Settings
        composable(NavRoutes.ACCOUNT_SECURITY) { backStackEntry ->
            val accountViewModel: AccountSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ACCOUNT_GRAPH
            )

            AccountSecurityScreen(
                viewModel = accountViewModel,
                onNavigateBack = {
                    inspectorViewModel.logNavigation("popBackStack", NavRoutes.ACCOUNT_PROFILE, NavRoutes.ACCOUNT_GRAPH)
                    navController.popBackStack()
                }
            )
        }

        // Team Management
        composable(NavRoutes.ACCOUNT_TEAM) { backStackEntry ->
            val accountViewModel: AccountSharedViewModel = backStackEntry.sharedViewModel(
                navController = navController,
                parentGraphRoute = NavRoutes.ACCOUNT_GRAPH
            )

            AccountTeamScreen(
                viewModel = accountViewModel,
                onNavigateBack = {
                    inspectorViewModel.logNavigation("popBackStack", NavRoutes.ACCOUNT_PROFILE, NavRoutes.ACCOUNT_GRAPH)
                    navController.popBackStack()
                }
            )
        }
    }
}
