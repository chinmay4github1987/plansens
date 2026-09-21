package com.prasjaychi.plantsense.ui.trends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity
import com.prasjaychi.plantsense.ui.theme.Amber500
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo500
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Rose500
import com.prasjaychi.plantsense.ui.theme.Violet400
import com.prasjaychi.plantsense.ui.theme.Violet500
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual Palette for Chart Segments and Bars inspired by D3/Recharts color scales.
 */
val ChartPalette = listOf(
    Emerald500,
    Cyan500,
    Indigo500,
    Violet500,
    Amber500,
    Rose500,
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF97316),
    Color(0xFF8B5CF6)
)

enum class TrendGroupingMode(val label: String) {
    COMMON_NAME("Species / Common Name"),
    FAMILY("Botanical Family"),
    SEVERITY("Health Status")
}

data class PlantTypeFrequency(
    val typeName: String,
    val scientificName: String = "",
    val count: Int,
    val percentage: Float,
    val color: Color,
    val latestTimestamp: Long = 0L,
    val averageHealthScore: Int = 0
)

data class TimeSeriesDataPoint(
    val label: String,
    val timestamp: Long,
    val count: Int,
    val dateString: String
)

/**
 * Screen visualizing plant identification history trends from the Room database.
 * Provides interactive D3/Recharts-style SVG/Canvas charts:
 * 1. Frequency Distribution Bar Chart (Recharts-style responsive horizontal & vertical bars)
 * 2. Time-Series Trend Line & Area Chart (Frequency of analyses over time)
 * 3. Proportional Donut Chart (Botanical family & species market share)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantTrendsScreen(
    viewModel: PlantHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHealthTrends: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allPlants = uiState.items

    var selectedTabIndex by remember { mutableStateOf(0) }
    var groupingMode by remember { mutableStateOf(TrendGroupingMode.COMMON_NAME) }

    // Aggregate frequencies based on the chosen grouping mode
    val frequencies = remember(allPlants, groupingMode) {
        computeFrequencies(allPlants, groupingMode)
    }

    // Compute time series buckets (e.g. by day/scan sequence)
    val timeSeries = remember(allPlants) {
        computeTimeSeries(allPlants)
    }

    // Prepare health records for Canvas rendering
    val healthRecordsFromAnalyses = remember(allPlants) {
        allPlants.sortedBy { it.timestampMs }.mapIndexed { idx, plant ->
            PlantHealthRecordEntity(
                id = plant.id,
                plantId = plant.id,
                plantName = plant.commonName,
                scientificName = plant.scientificName,
                timestampMs = plant.timestampMs,
                formattedDate = plant.formattedDate,
                healthScore = plant.healthScore,
                vitalityStatus = if (plant.healthScore >= 80) "THRIVING" else if (plant.healthScore >= 60) "HEALTHY" else "NEEDS_ATTENTION",
                soilMoistureLevel = (0.45f + (idx % 3) * 0.15f).coerceIn(0.2f, 0.85f),
                soilMoistureStatus = "OPTIMAL",
                leafCondition = plant.severity,
                careNotes = plant.primaryCause
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Plant Analysis Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room Database Visualization • ${allPlants.size} Total Scans",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("trends_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHealthTrends,
                        modifier = Modifier.testTag("trends_open_canvas_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Open Canvas Health Trends",
                            tint = Emerald500
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View History Records",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (allPlants.isEmpty()) {
            EmptyTrendsPlaceholder(
                onNavigateToCamera = onNavigateToCamera,
                onResetDemo = { viewModel.resetDemoRecords() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .testTag("trends_content_column"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                // Header Metrics & Insight Strip
                item {
                    TrendsOverviewMetricsCard(
                        totalAnalyses = allPlants.size,
                        uniqueSpeciesCount = allPlants.map { it.commonName.trim() }.distinct().size,
                        uniqueFamiliesCount = allPlants.map { it.family.trim() }.distinct().size,
                        avgHealth = if (allPlants.isNotEmpty()) allPlants.map { it.healthScore }.average().toInt() else 0
                    )
                }

                // Canvas Health Visualizer Quick Link Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToHealthTrends() }
                            .testTag("canvas_health_trends_banner"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald500.copy(alpha = 0.10f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Emerald500),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Plant Health & Vitality Visualizer",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald600
                                    )
                                    Text(
                                        text = "Compose Canvas curve, moisture tracker & checkup log",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToHealthTrends,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Open Canvas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Grouping Selector
                item {
                    GroupingModeSelector(
                        selectedMode = groupingMode,
                        onModeSelected = { groupingMode = it }
                    )
                }

                // Visualizer Mode Tabs (Frequency Bars, Over-Time Trend Area, Donut Share, Health Canvas)
                item {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Emerald500
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Frequency", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Time Trend", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Donut", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text("Health Canvas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // Active Chart Visualization Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_chart_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (selectedTabIndex) {
                                0 -> {
                                    Text(
                                        text = "Frequency Distribution of Analyzed Plant Types",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Relative occurrence across all botanical diagnoses in Room",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    RechartsHorizontalBarChart(frequencies = frequencies)
                                }
                                1 -> {
                                    Text(
                                        text = "Plant Analysis Frequency Over Time",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Timeline progression of captured diagnoses and scans",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    RechartsAreaTimeSeriesChart(timeSeries = timeSeries)
                                }
                                2 -> {
                                    Text(
                                        text = "Botanical Composition & Category Share",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Proportional market share of diagnosed flora",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    RechartsDonutChart(frequencies = frequencies)
                                }
                                3 -> {
                                    Text(
                                        text = "Plant Health & Vitality Canvas Curve",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Interactive cubic bezier curve with gradient fill and scrubber",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PlantHealthScoreCanvasChart(
                                        records = healthRecordsFromAnalyses,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PlantMoistureCanvasChart(
                                        records = healthRecordsFromAnalyses,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Breakdown Legend / Data Table
                item {
                    Text(
                        text = "Detailed Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(frequencies) { item ->
                    FrequencyDetailRow(item = item)
                }
            }
        }
    }
}

/**
 * Top metrics summary card.
 */
@Composable
private fun TrendsOverviewMetricsCard(
    totalAnalyses: Int,
    uniqueSpeciesCount: Int,
    uniqueFamiliesCount: Int,
    avgHealth: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Emerald500.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TOTAL ANALYSES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald600
                )
                Text(
                    text = "$totalAnalyses",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scans recorded",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Emerald500.copy(alpha = 0.2f))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "UNIQUE SPECIES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan500
                )
                Text(
                    text = "$uniqueSpeciesCount",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$uniqueFamiliesCount families",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Emerald500.copy(alpha = 0.2f))
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "AVG HEALTH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber500
                )
                Text(
                    text = "$avgHealth%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Vitality score",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Filter chips for grouping criteria (Common name, Botanical Family, Severity).
 */
@Composable
private fun GroupingModeSelector(
    selectedMode: TrendGroupingMode,
    onModeSelected: (TrendGroupingMode) -> Unit
) {
    Column {
        Text(
            text = "Group Trends By",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendGroupingMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald500.copy(alpha = 0.15f),
                        selectedLabelColor = Emerald600
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

/**
 * Recharts/D3 inspired horizontal bar chart displaying plant frequencies.
 */
@Composable
private fun RechartsHorizontalBarChart(frequencies: List<PlantTypeFrequency>) {
    val maxCount = (frequencies.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        frequencies.forEach { item ->
            val fraction = (item.count.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1f)
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(durationMillis = 600),
                label = "bar_anim"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = item.color,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.typeName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${item.count} scans",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${String.format(Locale.US, "%.0f%%", item.percentage * 100)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom D3/Recharts styled gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(item.color.copy(alpha = 0.8f), item.color)
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * Recharts/D3 inspired Area and Line Time-Series chart showing scan frequencies over time.
 */
@Composable
private fun RechartsAreaTimeSeriesChart(timeSeries: List<TimeSeriesDataPoint>) {
    if (timeSeries.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Capture at least 2 scans across time to view progression curve.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = (timeSeries.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(timeSeries) {
                        detectTapGestures { tapOffset ->
                            val stepX = size.width / (timeSeries.size - 1).coerceAtLeast(1)
                            val index = (tapOffset.x / stepX).toInt().coerceIn(0, timeSeries.size - 1)
                            selectedPointIndex = index
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingBottom = 24.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val chartHeight = canvasHeight - paddingBottom - paddingTop

                // Draw horizontal grid lines (D3 cartesian grid style)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = paddingTop + (chartHeight / gridLines) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Compute points
                val stepX = canvasWidth / (timeSeries.size - 1).coerceAtLeast(1)
                val points = timeSeries.mapIndexed { index, point ->
                    val x = index * stepX
                    val normalizedY = (point.count.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val y = paddingTop + chartHeight * (1f - normalizedY)
                    Offset(x, y)
                }

                // Draw filled Area under the line (Recharts Area style)
                val areaPath = Path().apply {
                    moveTo(points.first().x, canvasHeight - paddingBottom)
                    points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(points.last().x, canvasHeight - paddingBottom)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Emerald500.copy(alpha = 0.35f),
                            Emerald500.copy(alpha = 0.03f)
                        ),
                        startY = paddingTop,
                        endY = canvasHeight - paddingBottom
                    )
                )

                // Draw smooth connecting stroke line (Recharts Line style)
                val linePath = Path().apply {
                    points.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = Emerald500,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw circular nodes
                points.forEachIndexed { i, pt ->
                    val isSelected = selectedPointIndex == i
                    val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()

                    drawCircle(
                        color = Color.White,
                        radius = radius + 2.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) Cyan500 else Emerald500,
                        radius = radius,
                        center = pt
                    )
                }
            }
        }

        // X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            timeSeries.forEachIndexed { index, item ->
                if (index == 0 || index == timeSeries.size / 2 || index == timeSeries.size - 1) {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Active point tooltip inspection
        selectedPointIndex?.let { idx ->
            if (idx in timeSeries.indices) {
                val point = timeSeries[idx]
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date: ${point.dateString}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${point.count} Cumulative Plant Scans",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recharts/D3 inspired Donut Chart representing proportional market share.
 */
@Composable
private fun RechartsDonutChart(frequencies: List<PlantTypeFrequency>) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier
                .size(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(frequencies) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var start = -90f
                            frequencies.forEachIndexed { index, item ->
                                val sweep = item.percentage * 360f
                                val normalizedStart = (start + 360f) % 360f
                                val normalizedEnd = (start + sweep + 360f) % 360f

                                val isInSegment = if (normalizedStart < normalizedEnd) {
                                    angle in normalizedStart..normalizedEnd
                                } else {
                                    angle >= normalizedStart || angle <= normalizedEnd
                                }

                                if (isInSegment) {
                                    selectedIndex = index
                                }
                                start += sweep
                            }
                        }
                    }
            ) {
                val strokeWidth = 32.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f
                frequencies.forEachIndexed { index, item ->
                    val sweepAngle = item.percentage * 360f
                    val isSelected = selectedIndex == index

                    drawArc(
                        color = if (isSelected) item.color.copy(alpha = 0.85f) else item.color,
                        startAngle = startAngle,
                        sweepAngle = (sweepAngle - 2f).coerceAtLeast(0.5f), // 2 degree gap for clean D3 segment separation
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth * 1.15f else strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Central Counter Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val activeItem = selectedIndex?.let { frequencies.getOrNull(it) }
                if (activeItem != null) {
                    Text(
                        text = "${String.format(Locale.US, "%.0f%%", activeItem.percentage * 100)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeItem.color
                    )
                    Text(
                        text = "${activeItem.count} scans",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "${frequencies.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Categories",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Legend Stack
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            frequencies.take(5).forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedIndex = if (isSelected) null else index }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = item.color,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.typeName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f%%", item.percentage * 100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) item.color else MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (frequencies.size > 5) {
                Text(
                    text = "+ ${frequencies.size - 5} more types below",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

/**
 * Breakdown row detailing specific plant category frequency, count, and vitality.
 */
@Composable
private fun FrequencyDetailRow(item: PlantTypeFrequency) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = item.color.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.typeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (item.scientificName.isNotBlank() && item.scientificName != item.typeName) {
                    Text(
                        text = item.scientificName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Average Vitality: ${item.averageHealthScore}%",
                    fontSize = 10.sp,
                    color = if (item.averageHealthScore >= 80) Emerald600 else Amber500
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = item.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${item.count} ${if (item.count == 1) "scan" else "scans"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "%.1f%% of total", item.percentage * 100),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Placeholder when no plant analysis history exists in Room.
 */
@Composable
private fun EmptyTrendsPlaceholder(
    onNavigateToCamera: () -> Unit,
    onResetDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Emerald500.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Trend Data Available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Capture plant photos or restore realistic demo records to visualize frequency patterns, species trends, and health timelines.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onNavigateToCamera,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Plant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onResetDemo,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore Demo Records", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Frequency aggregation helper.
 */
private fun computeFrequencies(
    plants: List<PlantAnalysisEntity>,
    groupingMode: TrendGroupingMode
): List<PlantTypeFrequency> {
    if (plants.isEmpty()) return emptyList()

    val total = plants.size.toFloat()

    val grouped = when (groupingMode) {
        TrendGroupingMode.COMMON_NAME -> plants.groupBy { it.commonName.trim() }
        TrendGroupingMode.FAMILY -> plants.groupBy { it.family.trim() }
        TrendGroupingMode.SEVERITY -> plants.groupBy {
            when (it.severity) {
                "OPTIMAL" -> "Optimal Vitality"
                "MILD" -> "Mild Stress"
                "MODERATE" -> "Moderate Stress"
                "CRITICAL" -> "Critical Pathology"
                else -> it.severity
            }
        }
    }

    return grouped.entries
        .sortedByDescending { it.value.size }
        .mapIndexed { index, entry ->
            val count = entry.value.size
            val avgHealth = entry.value.map { it.healthScore }.average().toInt()
            val scientificName = entry.value.firstOrNull()?.scientificName ?: ""
            val color = ChartPalette[index % ChartPalette.size]

            PlantTypeFrequency(
                typeName = entry.key,
                scientificName = scientificName,
                count = count,
                percentage = count / total,
                color = color,
                latestTimestamp = entry.value.maxOfOrNull { it.timestampMs } ?: 0L,
                averageHealthScore = avgHealth
            )
        }
}

/**
 * Time-series calculation helper to chart cumulative or daily analysis counts over time.
 */
private fun computeTimeSeries(plants: List<PlantAnalysisEntity>): List<TimeSeriesDataPoint> {
    if (plants.isEmpty()) return emptyList()

    val sorted = plants.sortedBy { it.timestampMs }
    val dayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Group plants chronologically or create sequential scan steps
    var runningTotal = 0
    return sorted.mapIndexed { index, plant ->
        runningTotal += 1
        TimeSeriesDataPoint(
            label = dayFormat.format(Date(plant.timestampMs)),
            timestamp = plant.timestampMs,
            count = runningTotal,
            dateString = fullDateFormat.format(Date(plant.timestampMs))
        )
    }
}
