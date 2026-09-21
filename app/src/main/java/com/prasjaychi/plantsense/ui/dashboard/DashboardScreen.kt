package com.prasjaychi.plantsense.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.ui.components.charts.RadarDimension
import com.prasjaychi.plantsense.ui.components.charts.RechartsAreaChart
import com.prasjaychi.plantsense.ui.components.charts.RechartsBarChart
import com.prasjaychi.plantsense.ui.components.charts.RechartsCategoryShare
import com.prasjaychi.plantsense.ui.components.charts.RechartsColors
import com.prasjaychi.plantsense.ui.components.charts.RechartsDataPoint
import com.prasjaychi.plantsense.ui.components.charts.RechartsDonutChartComponent
import com.prasjaychi.plantsense.ui.components.charts.RechartsRadarChart
import com.prasjaychi.plantsense.ui.components.charts.RechartsStatCard
import com.prasjaychi.plantsense.ui.theme.Amber500
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo500
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Indigo700
import com.prasjaychi.plantsense.ui.theme.Indigo800
import com.prasjaychi.plantsense.ui.theme.Indigo900
import com.prasjaychi.plantsense.ui.theme.Rose500
import com.prasjaychi.plantsense.ui.theme.Violet400
import com.prasjaychi.plantsense.ui.theme.Violet500
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DashboardTimeRange(val label: String, val days: Int) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
    ALL_TIME("All", 3650)
}

enum class DashboardDistributionTab(val label: String) {
    SPECIES("Species"),
    FAMILY("Family"),
    SEVERITY("Health Tier")
}

/**
 * Modern Botanical Health & Navigation Dashboard Screen featuring Recharts-inspired UI components
 * wired directly to the Room database persistence layer.
 */
@Composable
fun DashboardScreen(
    onNavigateToOrderFlow: () -> Unit,
    onNavigateToAccountFlow: () -> Unit,
    onNavigateToVisualizer: () -> Unit,
    onNavigateToPlantCamera: () -> Unit = {},
    onNavigateToPlantAnalysis: () -> Unit = {},
    onNavigateToPlantLibrary: () -> Unit = {},
    onNavigateToPlantHistory: () -> Unit = {},
    onNavigateToPlantTrends: () -> Unit = {},
    onNavigateToHealthTrends: () -> Unit = {},
    onAnalyzeFrame: (android.graphics.Bitmap) -> Unit = {},
    viewModel: PlantHistoryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    val allPlants: List<PlantAnalysisEntity> = uiState.items

    var selectedTimeRange by remember { mutableStateOf(DashboardTimeRange.ALL_TIME) }
    var selectedDistTab by remember { mutableStateOf(DashboardDistributionTab.SPECIES) }
    var isArchitectureExpanded by remember { mutableStateOf(false) }

    // Aggregate Recharts Data from Room Database
    val timeSeriesData = remember(allPlants, selectedTimeRange) {
        computeVitalityTimeSeries(allPlants, selectedTimeRange.days)
    }

    val distributionCategories = remember(allPlants, selectedDistTab) {
        computeCategoricalShare(allPlants, selectedDistTab)
    }

    val rootCauseSlices = remember(allPlants) {
        computeRootCauseCategories(allPlants)
    }

    val radarDimensions = remember(allPlants) {
        computeRadarCareDimensions(allPlants)
    }

    val avgHealthScore = remember(allPlants) {
        if (allPlants.isNotEmpty()) allPlants.map { it.healthScore }.average().toInt() else 0
    }

    val attentionNeededCount = remember(allPlants) {
        allPlants.count { it.severity == "CRITICAL" || it.severity == "MODERATE" || it.healthScore < 70 }
    }

    val optimalCount = remember(allPlants) {
        allPlants.count { it.severity == "OPTIMAL" || it.healthScore >= 70 }
    }

    val healthScoresList: List<Float> = remember(allPlants) {
        if (allPlants.isEmpty()) listOf(0f, 0f) else allPlants.sortedBy { it.timestampMs }.map { it.healthScore.toFloat() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Botanical Hero Card with Recharts Dashboard Branding
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_hero_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Indigo900, Indigo700, Emerald600)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = Emerald400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "PlantSense Flora Engine",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Cyan400,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "AI Botanical Telemetry & Care",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        // Quick Seed / Refresh Icon
                        IconButton(
                            onClick = { viewModel.resetDemoRecords() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Demo Data",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Plant Health & Vitality Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Live telemetry and predictive trends derived from ${allPlants.size} diagnostic records persisted in Room database with cloud synchronization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToPlantCamera,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald500,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_scan_plant_button"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = onNavigateToPlantLibrary,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_library_button"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFlorist,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Library", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = onNavigateToPlantHistory,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_history_button"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("History", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Live CameraX Preview Component with frame capture staged for future AI analysis
        com.prasjaychi.plantsense.ui.components.MainCameraPreviewCard(
            onAnalyzeFrame = onAnalyzeFrame,
            onExpandToFullScanner = onNavigateToPlantCamera
        )

        // 2. Recharts Metric KPI Stat Cards Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RechartsStatCard(
                title = "Total Scans",
                value = "${allPlants.size}",
                subtitle = "Room entities",
                sparklineData = healthScoresList,
                delta = "+${(allPlants.size * 12.5).toInt()}%",
                isPositiveDelta = true,
                accentColor = Emerald500,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToPlantHistory() }
            )

            RechartsStatCard(
                title = "Avg Vitality",
                value = "$avgHealthScore%",
                subtitle = if (avgHealthScore >= 75) "Optimal State" else "Needs Attention",
                sparklineData = healthScoresList,
                delta = if (avgHealthScore >= 70) "+5.4%" else "-3.2%",
                isPositiveDelta = avgHealthScore >= 70,
                accentColor = if (avgHealthScore >= 75) Emerald500 else Amber500,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToPlantTrends() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RechartsStatCard(
                title = "Optimal Plants",
                value = "$optimalCount",
                subtitle = "Healthy foliage",
                sparklineData = listOf(2f, 4f, 5f, 6f, 8f),
                delta = "${if (allPlants.isNotEmpty()) (optimalCount * 100 / allPlants.size) else 0}% of total",
                isPositiveDelta = true,
                accentColor = Cyan500,
                modifier = Modifier.weight(1f)
            )

            RechartsStatCard(
                title = "Care Alerts",
                value = "$attentionNeededCount",
                subtitle = "Requires action",
                sparklineData = listOf(5f, 3f, 4f, 2f, 1f),
                delta = if (attentionNeededCount == 0) "Zero alerts" else "Needs review",
                isPositiveDelta = attentionNeededCount == 0,
                accentColor = if (attentionNeededCount == 0) Emerald500 else Rose500,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Primary Recharts Area & Spline Trend Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vitality_trend_chart_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Time Range Filter Bar (Recharts Toolbar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Plant Vitality Progression",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Time Range Pill Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DashboardTimeRange.values().forEach { range ->
                            val isSelected = selectedTimeRange == range
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedTimeRange = range }
                            ) {
                                Text(
                                    text = range.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                RechartsAreaChart(
                    dataPoints = timeSeriesData,
                    chartTitle = "Vitality Index Curve (${selectedTimeRange.label})",
                    chartSubtitle = "Tap or drag along curve to inspect recorded historical diagnoses",
                    primaryColor = Emerald500,
                    secondaryColor = Cyan400,
                    unit = "%",
                    referenceThreshold = 80f
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToHealthTrends,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_open_canvas_trends_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interactive Canvas Health Trends & Checkups", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Recharts Categorical Frequency Distribution Bar Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("categorical_distribution_chart_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categorical Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Tab selector
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DashboardDistributionTab.values().forEach { tab ->
                            val isSelected = selectedDistTab == tab
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Cyan500 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedDistTab = tab }
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                RechartsBarChart(
                    categories = distributionCategories,
                    chartTitle = "Frequency by ${selectedDistTab.label}",
                    chartSubtitle = "Occurrence distribution across Room botanical records"
                )
            }
        }

        // 5. Recharts Donut & Radar Chart Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Diagnosed Stress Factor Share",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Distribution of underlying care root causes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                RechartsDonutChartComponent(
                    slices = rootCauseSlices,
                    centerTitle = "${allPlants.size}",
                    centerSubtitle = "Total Flora Scans"
                )
            }
        }

        // 6. 5-Axis Botanical Care Vector Radar Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "5-Axis Botanical Health Vectors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aggregate care balance polygon across light, hydration, humidity, soil, and immunity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                RechartsRadarChart(
                    dimensions = radarDimensions,
                    polygonColor = Emerald500
                )
            }
        }

        // 7. Recent Diagnoses from Room Database
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Botanical Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "View All (${allPlants.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600,
                        modifier = Modifier.clickable { onNavigateToPlantHistory() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (allPlants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved plant records found in Room database.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allPlants.take(4).forEach { plant ->
                            RecentPlantItemRow(
                                plant = plant,
                                onClick = onNavigateToPlantHistory
                            )
                        }
                    }
                }
            }
        }

        // 8. Navigation Graph Architecture & Sub-Flows (Collapsible Section)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isArchitectureExpanded = !isArchitectureExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Navigation Architecture & Sub-Flows",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Order Wizard, Account Hub, & Graph Inspector",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { isArchitectureExpanded = !isArchitectureExpanded }) {
                        Icon(
                            imageVector = if (isArchitectureExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Architecture"
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isArchitectureExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToOrderFlow,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Order Wizard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToAccountFlow,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Account Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToVisualizer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Inspector", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentPlantItemRow(
    plant: PlantAnalysisEntity,
    onClick: () -> Unit
) {
    val isOptimal = plant.severity == "OPTIMAL" || plant.healthScore >= 70
    val badgeColor = if (isOptimal) Emerald500 else if (plant.healthScore >= 50) Amber500 else Rose500

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOptimal) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plant.commonName.ifBlank { "Botanical Sample" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${plant.family} • ${String.format(Locale.US, "%.0f%%", plant.matchConfidence * 100)} confidence",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${plant.healthScore}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ---------------- Helper Calculation Functions ----------------

private fun computeVitalityTimeSeries(
    plants: List<PlantAnalysisEntity>,
    daysLimit: Int
): List<RechartsDataPoint> {
    if (plants.isEmpty()) return emptyList()

    val now = System.currentTimeMillis()
    val cutoff = if (daysLimit >= 3650) 0L else now - (daysLimit * 24L * 60L * 60L * 1000L)
    val filtered = plants.filter { it.timestampMs >= cutoff }.sortedBy { it.timestampMs }

    val dataList = if (filtered.isEmpty()) plants.sortedBy { it.timestampMs } else filtered
    val dateFormat = SimpleDateFormat("MMM d", Locale.US)

    return dataList.mapIndexed { idx, entity ->
        RechartsDataPoint(
            label = dateFormat.format(Date(entity.timestampMs)),
            value = entity.healthScore.toFloat(),
            secondaryValue = idx + 1f,
            description = entity.commonName,
            category = entity.family,
            timestamp = entity.timestampMs,
            color = if (entity.healthScore >= 75) Emerald500 else if (entity.healthScore >= 50) Amber500 else Rose500
        )
    }
}

private fun computeCategoricalShare(
    plants: List<PlantAnalysisEntity>,
    tab: DashboardDistributionTab
): List<RechartsCategoryShare> {
    if (plants.isEmpty()) return emptyList()

    val total = plants.size.toFloat()

    val grouped = when (tab) {
        DashboardDistributionTab.SPECIES -> {
            plants.groupBy { it.commonName.trim().ifBlank { "Unidentified" } }
        }
        DashboardDistributionTab.FAMILY -> {
            plants.groupBy { it.family.trim().ifBlank { "Flora Family" } }
        }
        DashboardDistributionTab.SEVERITY -> {
            plants.groupBy {
                when {
                    it.healthScore >= 80 -> "Optimal Vitality (80-100%)"
                    it.healthScore >= 60 -> "Mild Stress (60-79%)"
                    it.healthScore >= 40 -> "Moderate Stress (40-59%)"
                    else -> "Critical Action Needed (<40%)"
                }
            }
        }
    }

    return grouped.entries
        .sortedByDescending { it.value.size }
        .take(6)
        .mapIndexed { idx, entry ->
            val color = RechartsColors[idx % RechartsColors.size]
            val count = entry.value.size
            RechartsCategoryShare(
                name = entry.key,
                count = count,
                percentage = count / total,
                color = color,
                subtext = "Avg ${entry.value.map { it.healthScore }.average().toInt()}% health"
            )
        }
}

private fun computeRootCauseCategories(plants: List<PlantAnalysisEntity>): List<RechartsCategoryShare> {
    if (plants.isEmpty()) {
        return listOf(
            RechartsCategoryShare("Optimal Balance", 1, 1.0f, Emerald500, "Healthy")
        )
    }

    val total = plants.size.toFloat()
    var overwater = 0
    var light = 0
    var humidity = 0
    var pest = 0
    var optimal = 0

    plants.forEach { p ->
        val text = (p.symptomsJoined + " " + p.primaryCause + " " + p.rootCauseCategory + " " + p.immediateIntervention).lowercase()
        when {
            p.severity == "OPTIMAL" || (p.severity != "CRITICAL" && p.healthScore >= 75) -> optimal++
            text.contains("water") || text.contains("moist") || text.contains("root rot") -> overwater++
            text.contains("light") || text.contains("sun") || text.contains("shade") -> light++
            text.contains("humid") || text.contains("crisp") || text.contains("dry") -> humidity++
            text.contains("pest") || text.contains("mite") || text.contains("fung") -> pest++
            else -> optimal++
        }
    }

    val list = mutableListOf<RechartsCategoryShare>()
    if (optimal > 0) list.add(RechartsCategoryShare("Optimal Flora", optimal, optimal / total, Emerald500))
    if (overwater > 0) list.add(RechartsCategoryShare("Watering Imbalance", overwater, overwater / total, Cyan500))
    if (light > 0) list.add(RechartsCategoryShare("Light Deficit", light, light / total, Amber500))
    if (humidity > 0) list.add(RechartsCategoryShare("Low Humidity", humidity, humidity / total, Violet500))
    if (pest > 0) list.add(RechartsCategoryShare("Fungal / Pest Threat", pest, pest / total, Rose500))

    return if (list.isEmpty()) listOf(RechartsCategoryShare("Optimal Balance", 1, 1.0f, Emerald500)) else list
}

private fun computeRadarCareDimensions(plants: List<PlantAnalysisEntity>): List<RadarDimension> {
    if (plants.isEmpty()) {
        return listOf(
            RadarDimension("Light Adequacy", 85f),
            RadarDimension("Moisture Level", 80f),
            RadarDimension("Humidity", 75f),
            RadarDimension("Soil Quality", 90f),
            RadarDimension("Immunity", 85f)
        )
    }

    val avgHealth = plants.map { it.healthScore }.average().toFloat()
    val issuesText = plants.joinToString(" ") { it.symptomsJoined + " " + it.primaryCause }.lowercase()

    val lightScore = if (issuesText.contains("light") || issuesText.contains("sun")) (avgHealth * 0.75f).coerceIn(40f, 95f) else (avgHealth * 1.05f).coerceIn(60f, 98f)
    val moistureScore = if (issuesText.contains("water") || issuesText.contains("rot")) (avgHealth * 0.7f).coerceIn(35f, 95f) else (avgHealth * 1.02f).coerceIn(60f, 98f)
    val humidityScore = if (issuesText.contains("humid") || issuesText.contains("dry")) (avgHealth * 0.8f).coerceIn(40f, 95f) else (avgHealth * 0.95f).coerceIn(55f, 95f)
    val soilScore = (avgHealth * 0.98f).coerceIn(50f, 98f)
    val immunityScore = if (issuesText.contains("pest") || issuesText.contains("fung")) (avgHealth * 0.65f).coerceIn(30f, 95f) else (avgHealth * 1.08f).coerceIn(65f, 100f)

    return listOf(
        RadarDimension("Light Level", lightScore),
        RadarDimension("Hydration", moistureScore),
        RadarDimension("Humidity", humidityScore),
        RadarDimension("Soil Health", soilScore),
        RadarDimension("Pathogen Defense", immunityScore)
    )
}
