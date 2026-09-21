package com.prasjaychi.plantsense.ui

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prasjaychi.plantsense.navigation.NavRoutes
import com.prasjaychi.plantsense.navigation.accountGraph
import com.prasjaychi.plantsense.navigation.bottomNavItems
import com.prasjaychi.plantsense.navigation.orderGraph
import com.prasjaychi.plantsense.ui.components.AppBottomNavigation
import com.prasjaychi.plantsense.ui.components.GraphBreadcrumbBar
import com.prasjaychi.plantsense.ui.components.NavInspectorBottomSheet
import com.prasjaychi.plantsense.ui.components.QuickActionsFab
import com.prasjaychi.plantsense.ui.care.PlantWateringLogScreen
import com.prasjaychi.plantsense.ui.camera.PlantAnalysisResultScreen
import com.prasjaychi.plantsense.ui.camera.PlantCameraScreen
import com.prasjaychi.plantsense.ui.dashboard.DashboardScreen
import com.prasjaychi.plantsense.ui.history.PlantHistoryScreen
import com.prasjaychi.plantsense.ui.library.PlantLibraryScreen
import com.prasjaychi.plantsense.ui.trends.PlantTrendsScreen
import com.prasjaychi.plantsense.ui.trends.PlantHealthTrendsScreen
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.visualizer.GraphVisualizerScreen
import com.prasjaychi.plantsense.viewmodel.PlantLibraryViewModel
import com.prasjaychi.plantsense.viewmodel.AccountSharedViewModel
import com.prasjaychi.plantsense.viewmodel.NavInspectorViewModel
import com.prasjaychi.plantsense.viewmodel.OrderSharedViewModel
import com.prasjaychi.plantsense.viewmodel.PlantAnalysisViewModel
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel
import com.prasjaychi.plantsense.viewmodel.PlantHealthTrendsViewModel
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.database.PlantAnalysisRepository
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavHubApp(
    inspectorViewModel: NavInspectorViewModel = viewModel(),
    plantAnalysisViewModel: PlantAnalysisViewModel = viewModel(),
    plantHistoryViewModel: PlantHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: NavRoutes.DASHBOARD
    val parentGraphRoute = currentDestination?.parent?.route

    val inspectorState by inspectorViewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()

    // Sync current destination & graph with inspector
    LaunchedEffect(currentRoute, parentGraphRoute) {
        val count = try {
            navController.currentBackStack.value.size
        } catch (_: Exception) {
            1
        }
        inspectorViewModel.updateBackstackDepth(count)
    }

    // Try to safely get live state JSON from any active shared ViewModel
    val orderStateJson: String? = remember(currentRoute, parentGraphRoute) {
        if (parentGraphRoute == NavRoutes.ORDER_GRAPH) {
            try {
                val entry = navController.getBackStackEntry(NavRoutes.ORDER_GRAPH)
                val vm = androidx.lifecycle.ViewModelProvider(entry)[OrderSharedViewModel::class.java]
                val s = vm.uiState.value
                "{\n  tier: \"${s.selectedTier.title}\",\n  billing: \"${s.billingCycle.name}\",\n  addons: [${s.enabledAddonIds.joinToString()}],\n  monthly: $${s.finalMonthlyCost},\n  workspace: \"${s.workspaceName}\"\n}"
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val accountStateJson: String? = remember(currentRoute, parentGraphRoute) {
        if (parentGraphRoute == NavRoutes.ACCOUNT_GRAPH) {
            try {
                val entry = navController.getBackStackEntry(NavRoutes.ACCOUNT_GRAPH)
                val vm = androidx.lifecycle.ViewModelProvider(entry)[AccountSharedViewModel::class.java]
                val s = vm.uiState.value
                "{\n  user: \"${s.fullName}\",\n  role: \"${s.titleRole}\",\n  2fa: ${s.twoFactorEnabled},\n  timeout: ${s.sessionTimeoutMinutes}m,\n  teamCount: ${s.teamMembers.size}\n}"
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val orderAddonsCount: Int = remember(currentRoute, parentGraphRoute) {
        if (parentGraphRoute == NavRoutes.ORDER_GRAPH) {
            try {
                val entry = navController.getBackStackEntry(NavRoutes.ORDER_GRAPH)
                val vm = androidx.lifecycle.ViewModelProvider(entry)[OrderSharedViewModel::class.java]
                vm.uiState.value.enabledAddonIds.size
            } catch (_: Exception) {
                0
            }
        } else 0
    }

    val teamMembersCount: Int = remember(currentRoute, parentGraphRoute) {
        if (parentGraphRoute == NavRoutes.ACCOUNT_GRAPH) {
            try {
                val entry = navController.getBackStackEntry(NavRoutes.ACCOUNT_GRAPH)
                val vm = androidx.lifecycle.ViewModelProvider(entry)[AccountSharedViewModel::class.java]
                vm.uiState.value.teamMembers.size
            } catch (_: Exception) {
                0
            }
        } else 0
    }

    // Determine TopAppBar title
    val screenTitle = when {
        currentRoute == NavRoutes.DASHBOARD -> "PlantSense Home"
        currentRoute == NavRoutes.PLANT_LIBRARY -> "Plant Library"
        currentRoute == NavRoutes.PLANT_HISTORY -> "History"
        currentRoute == NavRoutes.PLANT_CAMERA -> "Plant AI Scanner"
        currentRoute == NavRoutes.PLANT_ANALYSIS -> "Diagnosis & Care Plan"
        currentRoute == NavRoutes.PLANT_TRENDS -> "Health & Care Trends"
        currentRoute == NavRoutes.PLANT_HEALTH_TRENDS -> "Plant Health Trends"
        currentRoute == NavRoutes.VISUALIZER -> "Navigation Architecture"
        currentRoute == NavRoutes.ORDER_PLAN -> "Order: Plan Selection"
        currentRoute == NavRoutes.ORDER_ADDONS -> "Order: Add-ons"
        currentRoute == NavRoutes.ORDER_REVIEW -> "Order: Review & Deploy"
        currentRoute == NavRoutes.ORDER_CONFIRMATION -> "Order: Confirmed"
        currentRoute == NavRoutes.ACCOUNT_PROFILE -> "Account Profile"
        currentRoute == NavRoutes.ACCOUNT_EDIT -> "Edit Profile"
        currentRoute == NavRoutes.ACCOUNT_SECURITY -> "Security Settings"
        currentRoute == NavRoutes.ACCOUNT_TEAM -> "Workspace Team"
        else -> "PlantSense"
    }

    val isTopLevelRoute = currentRoute in listOf(
        NavRoutes.DASHBOARD,
        NavRoutes.PLANT_LIBRARY,
        NavRoutes.PLANT_HISTORY
    )
    val canNavigateBack = !isTopLevelRoute

    var showGlobalReminderPreferences by remember { mutableStateOf(false) }

    if (showGlobalReminderPreferences) {
        com.prasjaychi.plantsense.ui.preferences.GlobalReminderPreferencesDialog(
            onDismiss = { showGlobalReminderPreferences = false }
        )
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = {
                                inspectorViewModel.logNavigation("popBackStack", "previous", parentGraphRoute)
                                navController.popBackStack()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showGlobalReminderPreferences = true },
                            modifier = Modifier.testTag("open_global_reminder_preferences_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Watering Reminder Preferences",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = {
                            coroutineScope.launch {
                                inspectorViewModel.toggleInspectorSheet(true)
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text("${inspectorState.backstackDepth}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Inspect Navigation & Shared ViewModels",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Live breadcrumb bar
                GraphBreadcrumbBar(
                    currentRoute = currentRoute,
                    parentGraphRoute = parentGraphRoute,
                    backstackDepth = inspectorState.backstackDepth,
                    onInspectClick = {
                        coroutineScope.launch {
                            inspectorViewModel.toggleInspectorSheet(true)
                        }
                    }
                )
            }
        },
        bottomBar = {
            AppBottomNavigation(
                navController = navController,
                currentDestination = currentDestination,
                onNavigate = { item ->
                    inspectorViewModel.logNavigation(
                        actionType = "navigate",
                        destination = item.route,
                        parentGraph = if (item.isNestedGraph) item.route else null,
                        details = "Bottom Navigation switch"
                    )
                },
                orderAddonsCount = orderAddonsCount,
                teamMembersCount = teamMembersCount,
                backstackDepth = inspectorState.backstackDepth
            )
        },
        floatingActionButton = {
            if (currentRoute in listOf(
                    NavRoutes.DASHBOARD,
                    NavRoutes.PLANT_LIBRARY,
                    NavRoutes.PLANT_HISTORY,
                    NavRoutes.PLANT_TRENDS,
                    NavRoutes.PLANT_HEALTH_TRENDS,
                    NavRoutes.VISUALIZER,
                    null
                )
            ) {
                QuickActionsFab(
                    historyViewModel = plantHistoryViewModel,
                    onScanPlant = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_CAMERA, null, "From Quick Actions FAB Scan")
                        navController.navigate(NavRoutes.PLANT_CAMERA)
                    },
                    onOpenFullWateringLog = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_WATERING_LOG, null, "From Quick Actions FAB Watering Log")
                        navController.navigate(NavRoutes.PLANT_WATERING_LOG)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.DASHBOARD,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Root Destination 1: Dashboard
            composable(NavRoutes.DASHBOARD) { backStackEntry ->
                val historyVm = plantHistoryViewModel ?: viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = PlantHistoryViewModel.provideFactory(context.applicationContext as Application)
                )
                DashboardScreen(
                    onNavigateToOrderFlow = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.ORDER_GRAPH, null, "From Dashboard card")
                        navController.navigate(NavRoutes.ORDER_GRAPH)
                    },
                    onNavigateToAccountFlow = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.ACCOUNT_GRAPH, null, "From Dashboard card")
                        navController.navigate(NavRoutes.ACCOUNT_GRAPH)
                    },
                    onNavigateToVisualizer = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.VISUALIZER, null, "From Dashboard card")
                        navController.navigate(NavRoutes.VISUALIZER)
                    },
                    onNavigateToPlantCamera = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_CAMERA, null, "From Dashboard Plant Scanner")
                        navController.navigate(NavRoutes.PLANT_CAMERA)
                    },
                    onNavigateToPlantAnalysis = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_ANALYSIS, null, "From Dashboard Results")
                        navController.navigate(NavRoutes.PLANT_ANALYSIS)
                    },
                    onNavigateToPlantLibrary = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_LIBRARY, null, "From Dashboard Library")
                        navController.navigate(NavRoutes.PLANT_LIBRARY)
                    },
                    onNavigateToPlantHistory = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HISTORY, null, "From Dashboard History")
                        navController.navigate(NavRoutes.PLANT_HISTORY)
                    },
                    onNavigateToPlantTrends = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_TRENDS, null, "From Dashboard Trends")
                        navController.navigate(NavRoutes.PLANT_TRENDS)
                    },
                    onNavigateToHealthTrends = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HEALTH_TRENDS, null, "From Dashboard Health Trends")
                        navController.navigate(NavRoutes.PLANT_HEALTH_TRENDS)
                    },
                    onAnalyzeFrame = { capturedBitmap ->
                        plantAnalysisViewModel.setCapturedBitmap(capturedBitmap)
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_ANALYSIS, null, "From Dashboard Live Camera Frame Capture")
                        navController.navigate(NavRoutes.PLANT_ANALYSIS)
                    },
                    viewModel = historyVm
                )
            }

            // Destination: Plant Library Botanical Encyclopedia Screen
            composable(NavRoutes.PLANT_LIBRARY) { backStackEntry ->
                val libraryViewModel: PlantLibraryViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = PlantLibraryViewModel.provideFactory(context.applicationContext as Application)
                )
                PlantLibraryScreen(
                    viewModel = libraryViewModel,
                    onNavigateToCamera = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_CAMERA, null, "From Library to Camera")
                        navController.navigate(NavRoutes.PLANT_CAMERA)
                    },
                    onNavigateToHistory = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HISTORY, null, "From Library to History")
                        navController.navigate(NavRoutes.PLANT_HISTORY)
                    }
                )
            }

            // Nested Graph 1: Order Wizard with Shared ViewModel
            orderGraph(navController, inspectorViewModel)

            // Nested Graph 2: Account & Settings with Shared ViewModel
            accountGraph(navController, inspectorViewModel)

            // Root Destination 2: Graph Inspector & Sandbox
            composable(NavRoutes.VISUALIZER) {
                GraphVisualizerScreen(
                    navController = navController,
                    inspectorViewModel = inspectorViewModel
                )
            }

            // Plant AI Camera Screen
            composable(NavRoutes.PLANT_CAMERA) {
                PlantCameraScreen(
                    onNavigateBack = {
                        inspectorViewModel.logNavigation("popBackStack", NavRoutes.DASHBOARD, null, "From Plant Camera")
                        navController.popBackStack()
                    },
                    onNavigateToAnalysis = { capturedBitmap ->
                        plantAnalysisViewModel.setCapturedBitmap(capturedBitmap)
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_ANALYSIS, null, "From Plant Camera to Diagnosis")
                        navController.navigate(NavRoutes.PLANT_ANALYSIS)
                    }
                )
            }

            // Plant AI Analysis & Root Cause Diagnosis Destination Screen
            composable(NavRoutes.PLANT_ANALYSIS) {
                PlantAnalysisResultScreen(
                    viewModel = plantAnalysisViewModel,
                    onNavigateBack = {
                        inspectorViewModel.logNavigation("popBackStack", "previous", null, "Back from Diagnosis")
                        navController.popBackStack()
                    },
                    onRetakePhoto = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_CAMERA, null, "Retake Photo from Diagnosis")
                        navController.navigate(NavRoutes.PLANT_CAMERA) {
                            popUpTo(NavRoutes.PLANT_CAMERA) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.DASHBOARD, null, "Return to Dashboard")
                        navController.navigate(NavRoutes.DASHBOARD) {
                            popUpTo(NavRoutes.DASHBOARD) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HISTORY, null, "From Analysis to History")
                        navController.navigate(NavRoutes.PLANT_HISTORY)
                    },
                    onSaveToHistory = { report ->
                        coroutineScope.launch {
                            val db = PlantDatabase.getDatabase(context)
                            val syncManager = com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager.getInstance(context)
                            val repo = PlantAnalysisRepository(db.plantAnalysisDao(), syncManager)
                            repo.saveReport(report)
                        }
                    }
                )
            }

            // Plant Analysis History Destination Screen (Room Persistence)
            composable(NavRoutes.PLANT_HISTORY) { backStackEntry ->
                val historyVm = plantHistoryViewModel ?: viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = PlantHistoryViewModel.provideFactory(context.applicationContext as Application)
                )
                PlantHistoryScreen(
                    viewModel = historyVm,
                    onSelectAnalysis = { entity ->
                        plantAnalysisViewModel.loadFromEntity(entity)
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_ANALYSIS, null, "From History: ${entity.commonName}")
                        navController.navigate(NavRoutes.PLANT_ANALYSIS)
                    },
                    onNavigateToCamera = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_CAMERA, null, "From History to Camera")
                        navController.navigate(NavRoutes.PLANT_CAMERA)
                    },
                    onNavigateToTrends = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_TRENDS, null, "From History to Trends")
                        navController.navigate(NavRoutes.PLANT_TRENDS)
                    }
                )
            }

            // Plant Identification Trends & Visualizations Screen (D3 / Recharts inspired)
            composable(NavRoutes.PLANT_TRENDS) { backStackEntry ->
                val historyVm = plantHistoryViewModel ?: viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = PlantHistoryViewModel.provideFactory(context.applicationContext as Application)
                )
                PlantTrendsScreen(
                    viewModel = historyVm,
                    onNavigateBack = {
                        inspectorViewModel.logNavigation("popBackStack", "previous", null, "Back from Trends")
                        navController.popBackStack()
                    },
                    onNavigateToCamera = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_CAMERA, null, "From Trends to Camera")
                        navController.navigate(NavRoutes.PLANT_CAMERA)
                    },
                    onNavigateToHistory = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HISTORY, null, "From Trends to History")
                        navController.navigate(NavRoutes.PLANT_HISTORY)
                    },
                    onNavigateToHealthTrends = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HEALTH_TRENDS, null, "From Trends to Health Trends Canvas")
                        navController.navigate(NavRoutes.PLANT_HEALTH_TRENDS)
                    }
                )
            }

            // Plant Health Trends Screen (Compose Canvas visualization of Room records)
            composable(NavRoutes.PLANT_HEALTH_TRENDS) { backStackEntry ->
                val healthTrendsVm: PlantHealthTrendsViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = PlantHealthTrendsViewModel.provideFactory(context.applicationContext as Application)
                )
                PlantHealthTrendsScreen(
                    viewModel = healthTrendsVm,
                    onNavigateBack = {
                        inspectorViewModel.logNavigation("popBackStack", "previous", null, "Back from Health Trends")
                        navController.popBackStack()
                    },
                    onNavigateToHistory = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HISTORY, null, "From Health Trends to History")
                        navController.navigate(NavRoutes.PLANT_HISTORY)
                    }
                )
            }

            // Plant Watering Log Destination Screen
            composable(NavRoutes.PLANT_WATERING_LOG) {
                PlantWateringLogScreen(
                    analysisViewModel = plantAnalysisViewModel,
                    historyViewModel = plantHistoryViewModel,
                    onNavigateBack = {
                        inspectorViewModel.logNavigation("popBackStack", "previous", null, "Back from Watering Log")
                        navController.popBackStack()
                    },
                    onNavigateToHistory = {
                        inspectorViewModel.logNavigation("navigate", NavRoutes.PLANT_HISTORY, null, "From Watering Log to History")
                        navController.navigate(NavRoutes.PLANT_HISTORY)
                    }
                )
            }
        }

        // Live Inspector Bottom Sheet
        if (inspectorState.isInspectorSheetVisible) {
            NavInspectorBottomSheet(
                sheetState = sheetState,
                currentRoute = currentRoute,
                parentGraphRoute = parentGraphRoute,
                backstackDepth = inspectorState.backstackDepth,
                eventLogs = inspectorState.eventLogs,
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        inspectorViewModel.toggleInspectorSheet(false)
                    }
                },
                onClearLogs = { inspectorViewModel.clearLogs() },
                orderStateJson = orderStateJson,
                accountStateJson = accountStateJson
            )
        }
    }
}
