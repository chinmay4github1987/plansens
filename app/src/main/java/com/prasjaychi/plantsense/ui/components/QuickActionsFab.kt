package com.prasjaychi.plantsense.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel

/**
 * Expandable 'Quick Actions' Floating Action Button (FAB) placed within the Scaffold UI.
 * Provides rapid one-tap access to:
 * 1. Log Watering
 * 2. Scan Plant
 * 3. Add Health Note
 */
@Composable
fun QuickActionsFab(
    historyViewModel: PlantHistoryViewModel,
    onScanPlant: () -> Unit,
    onOpenFullWateringLog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showLogWateringDialog by remember { mutableStateOf(false) }
    var showAddHealthNoteDialog by remember { mutableStateOf(false) }

    val historyUiState by historyViewModel.uiState.collectAsState()
    val plants = historyUiState.items

    // Smooth rotational animation for the FAB icon
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed Dial Action Items (Visible when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                        slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { it / 2 } +
                        expandVertically(),
                exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                        slideOutVertically(spring(stiffness = Spring.StiffnessHigh)) { it / 2 } +
                        shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 4.dp, end = 2.dp)
                ) {
                    // Action 1: Add Health Note
                    QuickActionButtonItem(
                        label = "Add Health Note",
                        icon = Icons.Default.EditNote,
                        iconTint = Color.White,
                        containerColor = Indigo600,
                        testTag = "action_add_health_note",
                        onClick = {
                            isExpanded = false
                            showAddHealthNoteDialog = true
                        }
                    )

                    // Action 2: Scan Plant
                    QuickActionButtonItem(
                        label = "Scan Plant",
                        icon = Icons.Default.CameraAlt,
                        iconTint = Color.White,
                        containerColor = Emerald600,
                        testTag = "action_scan_plant",
                        onClick = {
                            isExpanded = false
                            onScanPlant()
                        }
                    )

                    // Action 3: Log Watering
                    QuickActionButtonItem(
                        label = "Log Watering",
                        icon = Icons.Default.WaterDrop,
                        iconTint = Color.White,
                        containerColor = Color(0xFF0284C7), // Bright Cyan/Sky Blue
                        testTag = "action_log_watering",
                        onClick = {
                            isExpanded = false
                            showLogWateringDialog = true
                        }
                    )
                }
            }

            // Primary 'Quick Actions' FAB trigger
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(16.dp),
                color = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else Emerald600,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .testTag("quick_actions_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isExpanded) "Close Quick Actions" else "Quick Actions",
                        tint = if (isExpanded) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle)
                    )
                    Text(
                        text = if (isExpanded) "Close" else "Quick Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isExpanded) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                }
            }
        }
    }

    // Modal Dialog: Quick Log Watering
    if (showLogWateringDialog) {
        QuickLogWateringDialog(
            plants = plants,
            onDismiss = { showLogWateringDialog = false },
            onConfirm = { selectedPlantId, presetAmount, notes ->
                historyViewModel.quickLogWatering(
                    plantId = selectedPlantId,
                    amountPreset = presetAmount,
                    notes = notes,
                    onCompleted = {
                        val plantName = plants.find { it.id == selectedPlantId }?.commonName ?: "Plant"
                        Toast.makeText(context, "Watering logged for $plantName!", Toast.LENGTH_SHORT).show()
                    }
                )
                showLogWateringDialog = false
            },
            onOpenFullScreen = onOpenFullWateringLog
        )
    }

    // Modal Dialog: Quick Add Health Note
    if (showAddHealthNoteDialog) {
        QuickAddHealthNoteDialog(
            plants = plants,
            onDismiss = { showAddHealthNoteDialog = false },
            onConfirm = { selectedPlantId, noteText, vitality, leafCondition ->
                historyViewModel.quickAddHealthNote(
                    plantId = selectedPlantId,
                    noteText = noteText,
                    vitalityStatus = vitality,
                    leafCondition = leafCondition,
                    onCompleted = {
                        val plantName = plants.find { it.id == selectedPlantId }?.commonName ?: "Plant"
                        Toast.makeText(context, "Health note saved for $plantName!", Toast.LENGTH_SHORT).show()
                    }
                )
                showAddHealthNoteDialog = false
            }
        )
    }
}

/**
 * Individual action item pill and icon within the Quick Actions Speed Dial.
 */
@Composable
private fun QuickActionButtonItem(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        // Text pill label
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Circular Icon button
        Surface(
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 4.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Fast one-tap Log Watering Dialog allowing users to select a plant,
 * choose watering volume, and record watering into the Room database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogWateringDialog(
    plants: List<PlantAnalysisEntity>,
    onDismiss: () -> Unit,
    onConfirm: (plantId: Long, presetAmount: String, notes: String) -> Unit,
    onOpenFullScreen: (() -> Unit)? = null
) {
    var selectedPlantId by remember {
        mutableLongStateOf(plants.firstOrNull()?.id ?: 0L)
    }
    var selectedPresetAmount by remember { mutableStateOf("Standard (250ml)") }
    var notesText by remember { mutableStateOf("") }

    val presetAmounts = listOf("Light Mist (50ml)", "Standard (250ml)", "Deep Soak (500ml)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("dialog_quick_log_watering")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Log Watering",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Record hydration event in Room DB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Plant Selection
                Text(
                    text = "Select Plant",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (plants.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFlorist,
                                contentDescription = null,
                                tint = Emerald600
                            )
                            Text(
                                text = "Default Botanical Collection",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(plants) { plant ->
                            val isSelected = plant.id == selectedPlantId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlantId = plant.id },
                                label = {
                                    Text(
                                        text = plant.commonName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Preset Volume Selection
                Text(
                    text = "Watering Amount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetAmounts.forEach { preset ->
                        val isSelected = preset == selectedPresetAmount
                        Surface(
                            onClick = { selectedPresetAmount = preset },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                maxLines = 2
                            )
                        }
                    }
                }

                // Optional Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Care note (Optional)") },
                    placeholder = { Text("E.g., Added liquid plant food") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watering_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onConfirm(selectedPlantId, selectedPresetAmount, notesText) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_log_watering_button")
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Watering Now", fontWeight = FontWeight.Bold)
                    }

                    if (onOpenFullScreen != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenFullScreen()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Full Watering Schedule", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fast one-tap Add Health Note Dialog allowing users to jot down
 * botanical condition logs, symptom observations, and vitality ratings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddHealthNoteDialog(
    plants: List<PlantAnalysisEntity>,
    onDismiss: () -> Unit,
    onConfirm: (plantId: Long, noteText: String, vitality: String, leafCondition: String) -> Unit
) {
    var selectedPlantId by remember {
        mutableLongStateOf(plants.firstOrNull()?.id ?: 0L)
    }
    var noteText by remember { mutableStateOf("") }
    var selectedVitality by remember { mutableStateOf("HEALTHY") }
    var selectedLeafCondition by remember { mutableStateOf("Vibrant") }

    val vitalityOptions = listOf("THRIVING", "HEALTHY", "NEEDS_ATTENTION", "CRITICAL")
    val quickNoteSnippets = listOf(
        "New leaf unfurling",
        "Pruned yellow foliage",
        "Repotted in fresh soil",
        "Moved closer to sunlight",
        "Soil feels dry to touch"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("dialog_quick_add_health_note")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Indigo600.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Indigo600,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Add Health Note",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Log symptoms & botanical observations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Plant Selection
                Text(
                    text = "Select Plant",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (plants.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFlorist,
                                contentDescription = null,
                                tint = Emerald600
                            )
                            Text(
                                text = "General Botanical Collection",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(plants) { plant ->
                            val isSelected = plant.id == selectedPlantId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlantId = plant.id },
                                label = {
                                    Text(
                                        text = plant.commonName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Indigo600,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Vitality Status
                Text(
                    text = "Current Vitality",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    vitalityOptions.forEach { status ->
                        val isSelected = status == selectedVitality
                        val color = when (status) {
                            "THRIVING" -> Emerald600
                            "HEALTHY" -> Emerald500
                            "NEEDS_ATTENTION" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }
                        Surface(
                            onClick = { selectedVitality = status },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Observation / Health Note") },
                    placeholder = { Text("Describe changes, leaf texture, or care steps...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("health_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Snippet Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Quick suggestions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickNoteSnippets) { snippet ->
                            Surface(
                                onClick = {
                                    noteText = if (noteText.isBlank()) snippet else "$noteText. $snippet"
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "+ $snippet",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        val finalNote = if (noteText.isBlank()) "Routine health checkup - $selectedVitality" else noteText
                        onConfirm(selectedPlantId, finalNote, selectedVitality, selectedLeafCondition)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_add_health_note_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Health Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
