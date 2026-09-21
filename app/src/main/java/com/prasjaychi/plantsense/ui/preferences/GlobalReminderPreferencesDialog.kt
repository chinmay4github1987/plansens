package com.prasjaychi.plantsense.ui.preferences

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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager
import com.prasjaychi.plantsense.notifications.PlantWateringWorkScheduler
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import kotlinx.coroutines.launch

/**
 * Global configuration dialog enabling users to manage application-wide watering reminder
 * preferences and WorkManager background execution behavior.
 */
@Composable
fun GlobalReminderPreferencesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { WateringReminderPreferencesManager.getInstance(context) }
    val preferences by prefsManager.preferences.collectAsState()

    val activeWorkCount by PlantWateringWorkScheduler.observeActiveRemindersCount(context)
        .collectAsState(initial = 0)

    var masterEnabled by remember(preferences.masterRemindersEnabled) {
        mutableStateOf(preferences.masterRemindersEnabled)
    }
    var selectedHour by remember(preferences.defaultHour) {
        mutableIntStateOf(preferences.defaultHour)
    }
    var selectedMinute by remember(preferences.defaultMinute) {
        mutableIntStateOf(preferences.defaultMinute)
    }
    var selectedCadence by remember(preferences.defaultCadenceDays) {
        mutableIntStateOf(preferences.defaultCadenceDays)
    }
    var requireBatteryNotLow by remember(preferences.requireBatteryNotLow) {
        mutableStateOf(preferences.requireBatteryNotLow)
    }
    var soundEnabled by remember(preferences.notificationSound) {
        mutableStateOf(preferences.notificationSound)
    }
    var vibrationEnabled by remember(preferences.notificationVibration) {
        mutableStateOf(preferences.notificationVibration)
    }
    var quietHoursEnabled by remember(preferences.quietHoursEnabled) {
        mutableStateOf(preferences.quietHoursEnabled)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notifications permission denied", Toast.LENGTH_SHORT).show()
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
                        color = Emerald500.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Reminder Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WorkManager Background Engine",
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
                // Active WorkManager Tasks Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (masterEnabled) Emerald500.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active WorkManager Jobs",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (masterEnabled) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (masterEnabled) "$activeWorkCount plant reminder(s) actively enqueued in background" else "All background reminders currently paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (masterEnabled) Emerald600 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (masterEnabled) "$activeWorkCount Active" else "Paused",
                                color = if (masterEnabled) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Master Toggle Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (masterEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (masterEnabled) Emerald600 else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enable Reminders",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allow background WorkManager alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = masterEnabled,
                            onCheckedChange = { isChecked ->
                                masterEnabled = isChecked
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.testTag("master_reminder_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Emerald600,
                                checkedTrackColor = Emerald500.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Default Notification Time Preferences
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Default Preferred Time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val timePresets = listOf(
                        Triple(7, 0, "07:00 AM (Early)"),
                        Triple(8, 30, "08:30 AM (Morning)"),
                        Triple(9, 0, "09:00 AM (Default)"),
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
                                    .testTag("global_time_chip_$h"),
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
                                    .testTag("global_time_chip_$h"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }
                }

                // Default Interval Cadence
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Default Interval Cadence",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val cadencePresets = listOf(
                        Pair(1, "Daily"),
                        Pair(2, "2 Days"),
                        Pair(3, "3 Days"),
                        Pair(7, "Weekly"),
                        Pair(14, "Bi-weekly")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cadencePresets.forEach { (days, label) ->
                            val isSelected = selectedCadence == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCadence = days },
                                label = { Text(label, fontSize = 9.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("global_cadence_chip_$days"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald600
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // WorkManager Battery Constraint Switch
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
                                text = "Defer notifications if battery is critically low",
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

                // Sound & Vibration Toggles
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
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sound Alerts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        modifier = Modifier.testTag("sound_switch")
                    )
                }

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
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vibration Alerts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        modifier = Modifier.testTag("vibrate_switch")
                    )
                }

                // Quiet Hours Switch
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
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Quiet Hours (10 PM - 7 AM)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Suppress sound alerts overnight",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = quietHoursEnabled,
                        onCheckedChange = { quietHoursEnabled = it },
                        modifier = Modifier.testTag("quiet_hours_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = preferences.copy(
                        masterRemindersEnabled = masterEnabled,
                        defaultHour = selectedHour,
                        defaultMinute = selectedMinute,
                        defaultCadenceDays = selectedCadence,
                        requireBatteryNotLow = requireBatteryNotLow,
                        notificationSound = soundEnabled,
                        notificationVibration = vibrationEnabled,
                        quietHoursEnabled = quietHoursEnabled
                    )
                    prefsManager.saveAll(updated)

                    // Reschedule active reminders with WorkManager using updated preferences
                    coroutineScope.launch {
                        PlantWateringWorkScheduler.rescheduleAllActiveReminders(context)
                    }

                    Toast.makeText(
                        context,
                        "Preferences saved. WorkManager schedules updated.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                },
                modifier = Modifier.testTag("save_preferences_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save & Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_preferences_button")
            ) {
                Text("Close")
            }
        }
    )
}
