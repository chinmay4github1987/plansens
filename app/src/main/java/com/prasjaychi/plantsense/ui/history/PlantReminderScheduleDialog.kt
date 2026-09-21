package com.prasjaychi.plantsense.ui.history

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.database.PlantReminderRepository
import com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager
import com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager
import com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper
import com.prasjaychi.plantsense.notifications.PlantWateringWorkScheduler
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Interactive dialog allowing users to customize and schedule background notifications
 * for plant watering using WorkManager based on individual user preferences (preferred notification
 * time, custom interval cadence, battery conservation constraint, and custom care instructions).
 */
@Composable
fun PlantReminderScheduleDialog(
    plant: PlantAnalysisEntity,
    onDismiss: () -> Unit,
    onReminderConfigured: (scheduled: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { WateringReminderPreferencesManager.getInstance(context) }
    val globalPrefs by prefsManager.preferences.collectAsState()

    // Detect recommended interval from Room carePlan string
    val recommendedInterval = remember(plant.wateringSchedule) {
        PlantWateringNotificationHelper.parseWateringCadenceDays(plant.wateringSchedule).coerceAtLeast(1)
    }

    var selectedIntervalDays by remember { mutableIntStateOf(recommendedInterval) }
    var selectedHour by remember { mutableIntStateOf(globalPrefs.defaultHour) }
    var selectedMinute by remember { mutableIntStateOf(globalPrefs.defaultMinute) }
    var requireBatteryNotLow by remember { mutableStateOf(globalPrefs.requireBatteryNotLow) }
    var customUserNotes by remember { mutableStateOf(plant.userNotes) }

    // Load any existing per-plant reminder preferences from Room
    LaunchedEffect(plant.id) {
        val db = PlantDatabase.getDatabase(context)
        val existing = db.plantReminderDao().getReminderByPlantName(plant.commonName)
        if (existing != null) {
            selectedHour = existing.hour
            selectedMinute = existing.minute
            selectedIntervalDays = existing.repeatDays
        }
    }

    // Live observation of WorkManager status for this plant
    val isWorkActive by PlantWateringWorkScheduler.observeWorkStatus(context, plant.id)
        .collectAsState(initial = false)

    // Compute live next scheduled trigger date string based on user preferences
    val (computedNextMs, computedNextFormatted) = remember(selectedHour, selectedMinute, selectedIntervalDays, plant.lastWateredMs) {
        PlantWateringWorkScheduler.computeNextWateringDate(
            plant = plant,
            hour = selectedHour,
            minute = selectedMinute,
            repeatDays = selectedIntervalDays
        )
    }

    // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications permission required for background alerts", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Cyan500.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Cyan500,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Watering Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = plant.commonName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live WorkManager Schedule Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWorkActive) Emerald500.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isWorkActive) Emerald600 else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WorkManager Next Trigger",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWorkActive) Emerald600 else MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isWorkActive) Emerald600 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isWorkActive) "Enqueued" else "Ready",
                                    color = if (isWorkActive) Color.White else MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = computedNextFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Cadence: Every $selectedIntervalDays day(s) at ${formatAmPm(selectedHour, selectedMinute)}" +
                                    if (requireBatteryNotLow) " • Battery saver enabled" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Prescribed Care Plan Notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Cyan500.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Cyan500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diagnosis Plan: ${plant.wateringSchedule}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // User Preference 1: Interval Cadence (Days)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Repeat Interval",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (selectedIntervalDays > 1) selectedIntervalDays-- },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease interval", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$selectedIntervalDays day${if (selectedIntervalDays > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { if (selectedIntervalDays < 30) selectedIntervalDays++ },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase interval", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val cadenceChips = listOf(
                        Pair(1, "Daily"),
                        Pair(2, "2 Days"),
                        Pair(3, "3 Days"),
                        Pair(7, "Weekly"),
                        Pair(10, "10 Days"),
                        Pair(14, "Bi-weekly")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cadenceChips.take(3).forEach { (days, label) ->
                            val isSelected = selectedIntervalDays == days
                            val isRecommended = days == recommendedInterval
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedIntervalDays = days },
                                label = {
                                    Text(
                                        text = if (isRecommended) "$label ⭐" else label,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("interval_chip_$days"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cadenceChips.drop(3).forEach { (days, label) ->
                            val isSelected = selectedIntervalDays == days
                            val isRecommended = days == recommendedInterval
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedIntervalDays = days },
                                label = {
                                    Text(
                                        text = if (isRecommended) "$label ⭐" else label,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("interval_chip_$days"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }
                }

                // User Preference 2: Preferred Notification Time
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preferred Notification Time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val timePresets = listOf(
                        Triple(7, 0, "07:00 AM (Early)"),
                        Triple(8, 30, "08:30 AM (Morning)"),
                        Triple(9, 0, "09:00 AM (Standard)"),
                        Triple(12, 0, "12:00 PM (Noon)"),
                        Triple(18, 0, "06:00 PM (Evening)"),
                        Triple(20, 0, "08:00 PM (Night)")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timePresets.take(3).forEach { (h, m, label) ->
                            val isSelected = selectedHour == h && selectedMinute == m
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedHour = h
                                    selectedMinute = m
                                },
                                label = { Text(label.substringBefore(" "), fontSize = 10.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("time_chip_$h"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timePresets.drop(3).forEach { (h, m, label) ->
                            val isSelected = selectedHour == h && selectedMinute == m
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedHour = h
                                    selectedMinute = m
                                },
                                label = { Text(label.substringBefore(" "), fontSize = 10.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("time_chip_$h"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }
                }

                // User Preference 3: WorkManager Battery Saver Constraint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = Cyan500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Battery Saver Constraint",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "WorkManager waits if battery is low",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = requireBatteryNotLow,
                        onCheckedChange = { requireBatteryNotLow = it },
                        modifier = Modifier.testTag("battery_constraint_switch")
                    )
                }

                // User Preference 4: Custom Care Reminder Note
                OutlinedTextField(
                    value = customUserNotes,
                    onValueChange = { customUserNotes = it },
                    label = { Text("Care Note (Included in notification)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Check topsoil 2 inches dry before watering", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_reminder_note_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Immediate Notification Test Button
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        PlantWateringNotificationHelper.showImmediateWateringNotification(
                            context = context,
                            plantId = plant.id,
                            plantName = plant.commonName,
                            scientificName = plant.scientificName,
                            wateringSchedule = plant.wateringSchedule,
                            severity = plant.severity,
                            userNotes = customUserNotes
                        )
                        Toast.makeText(context, "Dispatched notification for ${plant.commonName}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_test_notification_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = Cyan500)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test Notification Now", fontSize = 12.sp, color = Cyan500)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    coroutineScope.launch {
                        val db = PlantDatabase.getDatabase(context)
                        val reminderRepo = PlantReminderRepository(db.plantReminderDao(), db.plantAnalysisDao())
                        val syncManager = PlantFirestoreSyncManager.getInstance(context)

                        reminderRepo.saveReminder(
                            context = context,
                            plant = plant.copy(userNotes = customUserNotes),
                            hour = selectedHour,
                            minute = selectedMinute,
                            repeatDays = selectedIntervalDays,
                            userNotes = customUserNotes,
                            requireBatteryNotLow = requireBatteryNotLow,
                            syncManager = syncManager
                        )
                    }
                    Toast.makeText(
                        context,
                        "Watering reminder scheduled with WorkManager for ${plant.commonName} (every $selectedIntervalDays day(s) at ${formatAmPm(selectedHour, selectedMinute)})",
                        Toast.LENGTH_LONG
                    ).show()
                    onReminderConfigured(true)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_schedule_reminder_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule with WorkManager", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        val db = PlantDatabase.getDatabase(context)
                        val reminderRepo = PlantReminderRepository(db.plantReminderDao(), db.plantAnalysisDao())
                        val syncManager = PlantFirestoreSyncManager.getInstance(context)
                        reminderRepo.cancelReminder(context, plant.id, plant.commonName, syncManager)
                    }
                    Toast.makeText(context, "Cancelled WorkManager reminder for ${plant.commonName}", Toast.LENGTH_SHORT).show()
                    onReminderConfigured(false)
                    onDismiss()
                },
                modifier = Modifier.testTag("cancel_schedule_reminder_button")
            ) {
                Text("Cancel Reminder", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
            }
        }
    )
}

private fun formatAmPm(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
}
