package com.prasjaychi.plantsense.ui.care

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Indigo800
import com.prasjaychi.plantsense.viewmodel.PlantAnalysisViewModel
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dedicated screen in the plant care section that allows users to manually record
 * exactly when they last watered a specific plant, updating the Room database record.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantWateringLogScreen(
    analysisViewModel: PlantAnalysisViewModel,
    historyViewModel: PlantHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val analysisUiState by analysisViewModel.uiState.collectAsState()
    val historyUiState by historyViewModel.uiState.collectAsState()

    val currentReport = analysisUiState.currentReport
    val activeEntityId = analysisUiState.activeEntityId

    // Selected plant to water. If activeEntityId is present, preselect it from history;
    // otherwise if history items exist, select the matching plant or the first item.
    val selectedPlantEntity = historyUiState.items.find { it.id == activeEntityId }
        ?: historyUiState.items.find { it.commonName.equals(currentReport.identification.commonName, ignoreCase = true) }
        ?: historyUiState.items.firstOrNull()

    var targetedEntity by remember(selectedPlantEntity?.id) { mutableStateOf(selectedPlantEntity) }

    // Date & Time picker states
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestampMs by remember {
        mutableLongStateOf(
            if (targetedEntity != null && targetedEntity!!.lastWateredMs > 0L) {
                targetedEntity!!.lastWateredMs
            } else {
                System.currentTimeMillis()
            }
        )
    }

    val displayDateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val roomStoreDateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    var notesText by remember { mutableStateOf("") }
    var selectedAmountPreset by remember { mutableStateOf("Deep Soak") }
    var soilMoistureBefore by remember { mutableStateOf("Dry (Top 2 inches)") }
    var hasLoggedSuccessfully by remember { mutableStateOf(false) }

    // Synchronize targeted entity with history changes if needed
    val matchedFromHistory = historyUiState.items.find { it.id == targetedEntity?.id }
    val displayEntity = matchedFromHistory ?: targetedEntity

    Scaffold(
        modifier = modifier.testTag("plant_watering_log_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Log Watering Event",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayEntity?.commonName ?: currentReport.identification.commonName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("watering_log_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("watering_log_to_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View all plants in history"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Notification Card
            AnimatedVisibility(visible = hasLoggedSuccessfully) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watering_logged_success_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald500.copy(alpha = 0.14f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Watering Log Saved to Database!",
                                fontWeight = FontWeight.Bold,
                                color = Emerald600,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Updated Room record: ${displayDateFormat.format(Date(selectedTimestampMs))}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Target Plant Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_plant_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Cyan500.copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Cyan500,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayEntity?.commonName ?: currentReport.identification.commonName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = displayEntity?.scientificName ?: currentReport.identification.scientificName,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Prescribed Cadence vs Last Watered
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Prescribed Cadence",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = displayEntity?.wateringSchedule ?: currentReport.carePlan.wateringSchedule,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val lastWateredStr = if (displayEntity != null && displayEntity.lastWateredMs > 0L) {
                        displayDateFormat.format(Date(displayEntity.lastWateredMs))
                    } else if (currentReport.carePlan.lastWateredMs > 0L) {
                        displayDateFormat.format(Date(currentReport.carePlan.lastWateredMs))
                    } else {
                        "No manual waterings logged yet"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Cyan500.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Cyan500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Current recorded: $lastWateredStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // If user has multiple saved plants in Room, show a plant switcher chip row
                    if (historyUiState.items.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Switch target plant:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            historyUiState.items.take(4).forEach { item ->
                                val isSelected = displayEntity?.id == item.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        targetedEntity = item
                                        if (item.lastWateredMs > 0L) {
                                            selectedTimestampMs = item.lastWateredMs
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.commonName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Cyan500.copy(alpha = 0.2f),
                                        selectedLabelColor = Cyan500
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Date and Time Pickers Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watering_datetime_picker_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "When did you water this plant?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select the exact date and time to maintain hydration cadence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Date Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val c = Calendar.getInstance().apply { timeInMillis = selectedTimestampMs }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = selectedTimestampMs
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        selectedTimestampMs = newCal.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("select_date_trigger")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Cyan500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Date",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedTimestampMs)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = "Change",
                                fontSize = 12.sp,
                                color = Cyan500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selected Time Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val c = Calendar.getInstance().apply { timeInMillis = selectedTimestampMs }
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = selectedTimestampMs
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        selectedTimestampMs = newCal.timeInMillis
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                            .testTag("select_time_trigger")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Cyan500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Time",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedTimestampMs)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = "Change",
                                fontSize = 12.sp,
                                color = Cyan500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Shortcuts: Just Now, Earlier Today, Yesterday
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedTimestampMs = System.currentTimeMillis() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_shortcut_now"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Just Now", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                }
                                selectedTimestampMs = c.timeInMillis
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Morning", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedTimestampMs = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Yesterday", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Watering Details / Presets
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hydration Volume & Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val amounts = listOf("Light Mist", "Moderate (250ml)", "Deep Soak", "Bottom Watered")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        amounts.forEach { amount ->
                            val isSel = selectedAmountPreset == amount
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedAmountPreset = amount },
                                label = { Text(amount, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(alpha = 0.2f),
                                    selectedLabelColor = Cyan500
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Substrate Moisture Prior to Watering",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val moistureLevels = listOf("Bone Dry", "Dry (Top 2 inches)", "Slightly Moist")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        moistureLevels.forEach { lvl ->
                            val isSel = soilMoistureBefore == lvl
                            FilterChip(
                                selected = isSel,
                                onClick = { soilMoistureBefore = lvl },
                                label = { Text(lvl, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional user note
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Care Notes (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("e.g., Added dilute organic kelp fertilizer", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watering_care_notes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Save Action Button
            Button(
                onClick = {
                    val formatted = roomStoreDateFormat.format(Date(selectedTimestampMs))
                    val targetId = displayEntity?.id ?: activeEntityId

                    if (targetId != null && targetId > 0L) {
                        historyViewModel.recordWatering(targetId, selectedTimestampMs, formatted)
                    }

                    analysisViewModel.recordWatering(
                        timestampMs = selectedTimestampMs,
                        formattedDate = formatted
                    )

                    hasLoggedSuccessfully = true

                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Watering recorded for ${displayEntity?.commonName ?: currentReport.identification.commonName}!"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_watering_log_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Watering Record to Database",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("return_to_care_plan_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Care Plan", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_history_from_water_log_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View History", fontSize = 12.sp)
                }
            }
        }
    }
}
