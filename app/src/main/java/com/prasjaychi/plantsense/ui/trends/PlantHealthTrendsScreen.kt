package com.prasjaychi.plantsense.ui.trends

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity
import com.prasjaychi.plantsense.ui.theme.Amber500
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Rose500
import com.prasjaychi.plantsense.viewmodel.HealthTrendMetric
import com.prasjaychi.plantsense.viewmodel.HealthTrendTimeRange
import com.prasjaychi.plantsense.viewmodel.PlantFilterOption
import com.prasjaychi.plantsense.viewmodel.PlantHealthTrendsViewModel
import com.prasjaychi.plantsense.viewmodel.TrendDirection

/**
 * Screen visualizing longitudinal plant health trends, vitality scores, and moisture levels from Room database.
 * Powered by custom Jetpack Compose Canvas charts with smooth bezier curves and touch scrubbing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantHealthTrendsScreen(
    viewModel: PlantHealthTrendsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogCheckupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Plant Health Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room Database Visualization • Compose Canvas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("health_trends_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLogCheckupDialog = true },
                        modifier = Modifier.testTag("log_checkup_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Health Checkup",
                            tint = Emerald500
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Records",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogCheckupDialog = true },
                containerColor = Emerald600,
                contentColor = Color.White,
                modifier = Modifier.testTag("log_health_checkup_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Log Checkup", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Emerald500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .testTag("plant_health_trends_list"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
            ) {
                // 1. KPI Metric Overview Banner
                item {
                    HealthTrendOverviewCards(
                        averageScore = uiState.averageHealthScore,
                        delta = uiState.healthTrendDelta,
                        direction = uiState.trendDirection,
                        totalCheckups = uiState.filteredRecords.size
                    )
                }

                // 2. Filter Controls: Time Range & Plant Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Time Range Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Window:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HealthTrendTimeRange.entries.forEach { range ->
                                FilterChip(
                                    selected = uiState.selectedTimeRange == range,
                                    onClick = { viewModel.selectTimeRange(range) },
                                    label = { Text(range.label, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald500.copy(alpha = 0.15f),
                                        selectedLabelColor = Emerald500
                                    )
                                )
                            }
                        }

                        // Plant Selection Horizontal Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(uiState.availablePlants) { plantOpt ->
                                val isSelected = uiState.selectedPlantId == plantOpt.plantId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectPlant(plantOpt.plantId) },
                                    label = {
                                        Text(
                                            text = plantOpt.displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (plantOpt.plantId == null) Icons.Default.FilterList else Icons.Default.Eco,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald600,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Primary Health Score Canvas Trend Chart Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("health_score_canvas_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Emerald500.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShowChart,
                                            contentDescription = null,
                                            tint = Emerald500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Health Score Trajectory",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Longitudinal Vitality Curve (0 - 100%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Emerald500.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${uiState.filteredRecords.size} Points",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Compose Canvas Chart
                            PlantHealthScoreCanvasChart(
                                records = uiState.filteredRecords,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. Hydration & Soil Moisture Canvas Chart Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("soil_moisture_canvas_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            PlantMoistureCanvasChart(
                                records = uiState.filteredRecords,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 5. Vitality Distribution Breakdown
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vitality_distribution_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            VitalityDistributionCanvasBar(
                                breakdown = uiState.vitalityBreakdown,
                                total = uiState.filteredRecords.size,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 6. Longitudinal Records List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recorded Health Checkpoints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room Persistence",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 7. Checkpoint Items
                items(uiState.filteredRecords.reversed(), key = { it.id }) { record ->
                    HealthRecordCheckpointCard(
                        record = record,
                        onDelete = { viewModel.deleteRecord(record.id) }
                    )
                }
            }
        }
    }

    // Log Health Checkup Dialog
    if (showLogCheckupDialog) {
        LogHealthCheckupDialog(
            availablePlants = uiState.availablePlants.filter { it.plantId != null },
            onDismiss = { showLogCheckupDialog = false },
            onConfirm = { plantId, plantName, scientificName, score, vitality, moisture, moistureText, leaf, notes ->
                viewModel.logHealthCheckup(
                    plantId = plantId,
                    plantName = plantName,
                    scientificName = scientificName,
                    healthScore = score,
                    vitalityStatus = vitality,
                    soilMoistureLevel = moisture,
                    soilMoistureStatus = moistureText,
                    leafCondition = leaf,
                    careNotes = notes
                )
                showLogCheckupDialog = false
            }
        )
    }
}

@Composable
private fun HealthTrendOverviewCards(
    averageScore: Int,
    delta: Int,
    direction: TrendDirection,
    totalCheckups: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Average Score Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Average Health",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$averageScore%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            averageScore >= 80 -> Emerald500
                            averageScore >= 60 -> Amber500
                            else -> Rose500
                        }
                    )
                }
                Text(
                    text = "${totalCheckups} total scans logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // Trend Direction Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Trajectory Delta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (direction) {
                            TrendDirection.IMPROVING -> Icons.AutoMirrored.Filled.TrendingUp
                            TrendDirection.DECLINING -> Icons.AutoMirrored.Filled.TrendingDown
                            TrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when (direction) {
                            TrendDirection.IMPROVING -> Emerald500
                            TrendDirection.DECLINING -> Rose500
                            TrendDirection.STABLE -> Cyan400
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (delta >= 0) "+$delta%" else "$delta%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (direction) {
                            TrendDirection.IMPROVING -> Emerald500
                            TrendDirection.DECLINING -> Rose500
                            TrendDirection.STABLE -> Cyan400
                        }
                    )
                }
                Text(
                    text = when (direction) {
                        TrendDirection.IMPROVING -> "Positive recovery"
                        TrendDirection.DECLINING -> "Needs attention"
                        TrendDirection.STABLE -> "Balanced vitality"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HealthRecordCheckpointCard(
    record: PlantHealthRecordEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_record_item_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = record.plantName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (record.vitalityStatus) {
                            "THRIVING" -> Emerald500.copy(alpha = 0.15f)
                            "HEALTHY" -> Cyan400.copy(alpha = 0.15f)
                            "NEEDS_ATTENTION" -> Amber500.copy(alpha = 0.15f)
                            else -> Rose500.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = record.vitalityStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (record.vitalityStatus) {
                                "THRIVING" -> Emerald500
                                "HEALTHY" -> Cyan400
                                "NEEDS_ATTENTION" -> Amber500
                                else -> Rose500
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${record.formattedDate} • Leaf: ${record.leafCondition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (record.careNotes.isNotBlank()) {
                    Text(
                        text = "Notes: ${record.careNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        record.healthScore >= 80 -> Emerald500.copy(alpha = 0.15f)
                        record.healthScore >= 60 -> Amber500.copy(alpha = 0.15f)
                        else -> Rose500.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = "${record.healthScore}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            record.healthScore >= 80 -> Emerald500
                            record.healthScore >= 60 -> Amber500
                            else -> Rose500
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Checkpoint",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogHealthCheckupDialog(
    availablePlants: List<PlantFilterOption>,
    onDismiss: () -> Unit,
    onConfirm: (
        plantId: Long,
        plantName: String,
        scientificName: String,
        score: Int,
        vitality: String,
        moisture: Float,
        moistureText: String,
        leaf: String,
        notes: String
    ) -> Unit
) {
    var selectedPlant by remember { mutableStateOf(availablePlants.firstOrNull()) }
    var healthScore by remember { mutableFloatStateOf(85f) }
    var selectedVitality by remember { mutableStateOf("HEALTHY") }
    var soilMoisture by remember { mutableFloatStateOf(0.55f) }
    var leafCondition by remember { mutableStateOf("Vibrant Green") }
    var careNotes by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = Emerald500)
                Text("Log Plant Health Checkup", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Plant Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPlant?.displayName ?: "Select Plant",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Plant") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        availablePlants.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.displayName) },
                                onClick = {
                                    selectedPlant = opt
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Health Score Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Health Score", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${healthScore.toInt()}%", fontWeight = FontWeight.Bold, color = Emerald500)
                    }
                    Slider(
                        value = healthScore,
                        onValueChange = {
                            healthScore = it
                            selectedVitality = when {
                                it >= 85 -> "THRIVING"
                                it >= 70 -> "HEALTHY"
                                it >= 50 -> "NEEDS_ATTENTION"
                                else -> "CRITICAL"
                            }
                        },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = Emerald500,
                            activeTrackColor = Emerald500
                        )
                    )
                }

                // Vitality Status Quick Chips
                Column {
                    Text("Vitality Status", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("THRIVING", "HEALTHY", "NEEDS_ATTENTION", "CRITICAL").forEach { status ->
                            FilterChip(
                                selected = selectedVitality == status,
                                onClick = { selectedVitality = status },
                                label = { Text(status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald500
                                )
                            )
                        }
                    }
                }

                // Soil Moisture Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Soil Moisture", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                soilMoisture > 0.7f -> "Wet (${(soilMoisture * 100).toInt()}%)"
                                soilMoisture > 0.4f -> "Optimal (${(soilMoisture * 100).toInt()}%)"
                                else -> "Dry (${(soilMoisture * 100).toInt()}%)"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Cyan400
                        )
                    }
                    Slider(
                        value = soilMoisture,
                        onValueChange = { soilMoisture = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan400,
                            activeTrackColor = Cyan400
                        )
                    )
                }

                // Leaf condition input
                OutlinedTextField(
                    value = leafCondition,
                    onValueChange = { leafCondition = it },
                    label = { Text("Leaf Condition") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes input
                OutlinedTextField(
                    value = careNotes,
                    onValueChange = { careNotes = it },
                    label = { Text("Checkup Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val plant = selectedPlant ?: availablePlants.firstOrNull()
                    val pId = plant?.plantId ?: 1L
                    val pName = plant?.displayName ?: "Indoor Plant"
                    val sName = plant?.scientificName ?: ""
                    val moistureStatus = when {
                        soilMoisture > 0.7f -> "WET"
                        soilMoisture > 0.4f -> "OPTIMAL"
                        else -> "DRY"
                    }
                    onConfirm(
                        pId,
                        pName,
                        sName,
                        healthScore.toInt(),
                        selectedVitality,
                        soilMoisture,
                        moistureStatus,
                        leafCondition,
                        careNotes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("Save to Room", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
