package com.prasjaychi.plantsense.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Route constants for the application navigation hierarchy.
 */
object NavRoutes {
    // Top-level PlantSense destinations
    const val DASHBOARD = "dashboard"
    const val HOME = "dashboard"
    const val PLANT_LIBRARY = "plant_library"
    const val PLANT_CAMERA = "plant_camera"
    const val PLANT_TRENDS = "plant_trends"
    const val PLANT_HEALTH_TRENDS = "plant_health_trends"
    const val PLANT_HISTORY = "plant_history"
    const val VISUALIZER = "graph_visualizer"
    const val PLANT_ANALYSIS = "plant_analysis_result"
    const val PLANT_WATERING_LOG = "plant_watering_log"

    // Nested Graph: Order Flow
    const val ORDER_GRAPH = "order_graph"
    const val ORDER_PLAN = "order/plan"
    const val ORDER_ADDONS = "order/addons"
    const val ORDER_REVIEW = "order/review"
    const val ORDER_CONFIRMATION = "order/confirmation"

    // Nested Graph: Account Settings Flow
    const val ACCOUNT_GRAPH = "account_graph"
    const val ACCOUNT_PROFILE = "account/profile"
    const val ACCOUNT_EDIT = "account/edit"
    const val ACCOUNT_SECURITY = "account/security"
    const val ACCOUNT_TEAM = "account/team"
}

/**
 * Top-level Bottom Navigation item descriptor for PlantSense.
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isNestedGraph: Boolean = false
) {
    data object Home : BottomNavItem(
        route = NavRoutes.DASHBOARD,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Library : BottomNavItem(
        route = NavRoutes.PLANT_LIBRARY,
        title = "Plant Library",
        selectedIcon = Icons.Filled.LocalFlorist,
        unselectedIcon = Icons.Outlined.LocalFlorist
    )

    data object History : BottomNavItem(
        route = NavRoutes.PLANT_HISTORY,
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    // Legacy aliases for backwards compatibility
    val Dashboard = Home

    data object Scan : BottomNavItem(
        route = NavRoutes.PLANT_CAMERA,
        title = "Scan Plant",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt
    )

    data object Trends : BottomNavItem(
        route = NavRoutes.PLANT_TRENDS,
        title = "Trends",
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart
    )

    data object Inspector : BottomNavItem(
        route = NavRoutes.VISUALIZER,
        title = "Inspector",
        selectedIcon = Icons.Filled.Hub,
        unselectedIcon = Icons.Outlined.Hub
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Library,
    BottomNavItem.History
)
