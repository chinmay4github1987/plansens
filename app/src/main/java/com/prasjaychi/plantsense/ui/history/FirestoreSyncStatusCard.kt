package com.prasjaychi.plantsense.ui.history

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.sync.ConnectionType
import com.prasjaychi.plantsense.data.sync.PlantSyncMode
import com.prasjaychi.plantsense.data.sync.PlantSyncState
import com.prasjaychi.plantsense.data.sync.PlantSyncStatus
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel

/**
 * High-craft Room Database Priority & Lazy Firestore Sync Status Card.
 * Prioritizes instant local persistence while managing background synchronization,
 * queue draining, and simulated network outages for offline testing.
 */
@Composable
fun FirestoreSyncStatusCard(
    viewModel: PlantHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val syncState by viewModel.syncState.collectAsState()
    var showLogsDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }

    if (showLogsDialog) {
        FirestoreSyncLogDialog(
            syncState = syncState,
            onDismiss = { showLogsDialog = false }
        )
    }

    if (showDeviceDialog) {
        DeviceSwitcherDialog(
            syncState = syncState,
            onSelectDevice = { deviceName ->
                viewModel.switchActiveDevice(deviceName)
                showDeviceDialog = false
                Toast.makeText(context, "Active device switched to $deviceName", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDeviceDialog = false }
        )
    }

    val totalPendingQueue = syncState.pendingOperationsCount +
            syncState.pendingPlantsCount +
            syncState.pendingRemindersCount

    val statusColor = when {
        syncState.isSimulatedOutage -> Color(0xFFF59E0B) // Amber for simulated outage
        !syncState.isOnline -> Color(0xFFF59E0B)
        syncState.syncStatus == PlantSyncStatus.SYNCING -> Cyan500
        syncState.syncStatus == PlantSyncStatus.ERROR -> Color(0xFFEF4444)
        totalPendingQueue > 0 -> Color(0xFF3B82F6) // Blue for pending lazy sync
        else -> Emerald500
    }

    val statusLabel = when {
        syncState.isSimulatedOutage -> "Network Outage (Room DB Priority)"
        !syncState.isOnline -> "Offline (Room Persistence Active)"
        syncState.syncStatus == PlantSyncStatus.SYNCING -> "Draining Sync Queue..."
        syncState.syncStatus == PlantSyncStatus.ERROR -> "Sync Warning (Room Safe)"
        totalPendingQueue > 0 -> "$totalPendingQueue Pending Lazy-Sync"
        else -> "Cloud Synced • Room Priority"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_sync_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status Icon, Title, Network Badge, and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (syncState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Cyan500,
                                    strokeWidth = 2.dp
                                )
                            } else if (syncState.isSimulatedOutage || !syncState.isOnline) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "Offline Mode",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (totalPendingQueue > 0) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "Pending Sync",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Synced",
                                    tint = Emerald500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Room DB & Lazy-Sync",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (syncState.isSimulatedOutage)
                                "Network outage active • Instant Room writes"
                            else if (totalPendingQueue > 0)
                                "$totalPendingQueue pending local mutation(s) queued for Firestore"
                            else if (syncState.lastSyncedFormatted != null)
                                "Last synced: ${syncState.lastSyncedFormatted}"
                            else
                                "Auto lazy-sync active • Multi-device enabled",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showLogsDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Sync Logs",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.syncWithCloud(forcePull = true)
                            Toast.makeText(context, "Draining sync queue and fetching cloud updates...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("sync_now_button"),
                        enabled = !syncState.isSyncing && syncState.isOnline
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Now",
                            tint = if (syncState.isSyncing || !syncState.isOnline) MaterialTheme.colorScheme.outline else Emerald600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ConnectivityManager Network Listener & Active Sync Mode Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED) Emerald500.copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = 0.08f),
                border = BorderStroke(
                    1.dp,
                    if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED) Emerald500.copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED) Emerald500.copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.18f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        syncState.syncMode == PlantSyncMode.LOCAL_ROOM_ONLY -> Icons.Default.Storage
                                        syncState.connectionType == ConnectionType.WIFI -> Icons.Default.Wifi
                                        else -> Icons.Default.CloudSync
                                    },
                                    contentDescription = null,
                                    tint = if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED) Emerald600 else Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED) "Cloud Firestore Sync Active" else "Local Room DB Active (Offline)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED) Emerald600 else Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = syncState.networkDescription,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (syncState.syncMode == PlantSyncMode.CLOUD_ENABLED)
                                    "Connected via ConnectivityManager • Changes auto-sync to cloud"
                                else
                                    "100% offline functionality • Scans persist in Room SQLite & queue for sync",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Network Outage Simulation Switch and Room Priority indicator
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (syncState.isSimulatedOutage) Icons.Default.WifiOff else Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (syncState.isSimulatedOutage) Color(0xFFF59E0B) else Emerald600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Simulate Network Outage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (syncState.isSimulatedOutage)
                                    "Offline toggle active: All plant records queue safely in Room"
                                else
                                    "Toggle to test switching between Room-only and Cloud Firestore sync",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Switch(
                        checked = syncState.isSimulatedOutage,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleSimulatedNetworkOutage(isChecked)
                            Toast.makeText(
                                context,
                                if (isChecked) "Network Outage simulated: Toggled to local Room DB mode."
                                else "Network restored: Toggled to Cloud Firestore synchronization.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF59E0B),
                            checkedTrackColor = Color(0xFFF59E0B).copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("network_outage_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device identity bar and quick sync simulation action
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDeviceDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Device: ${syncState.activeDeviceName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Tap to switch simulated device profile",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Simulate Cross-Device Event button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Indigo600.copy(alpha = 0.12f),
                        modifier = Modifier.clickable {
                            val samplePlants = listOf("Fiddle Leaf Fig", "Peace Lily", "Boston Fern", "Calathea Orbifolia")
                            val chosen = samplePlants.random()
                            viewModel.simulateRemoteDeviceSync(chosen)
                            Toast.makeText(context, "Received cloud diagnosis for '$chosen' from secondary device!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tablet,
                                contentDescription = null,
                                tint = Indigo600,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Simulate Device B",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Indigo600
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog displaying live Firestore synchronization and Room database activity logs.
 */
@Composable
fun FirestoreSyncLogDialog(
    syncState: PlantSyncState,
    onDismiss: () -> Unit
) {
    val totalPendingQueue = syncState.pendingOperationsCount +
            syncState.pendingPlantsCount +
            syncState.pendingRemindersCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Room-First Sync Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Counts summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Synced: ${syncState.syncedPlantsCount} plants",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Emerald600
                    )
                    Text(
                        text = "Pending Queue: $totalPendingQueue items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalPendingQueue > 0) Color(0xFFF59E0B) else Cyan500
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (syncState.syncLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sync events recorded yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(syncState.syncLogs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("Close", fontSize = 12.sp)
            }
        }
    )
}

/**
 * Dialog enabling device profile selection for testing multi-device synchronization.
 */
@Composable
fun DeviceSwitcherDialog(
    syncState: PlantSyncState,
    onSelectDevice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = Indigo600,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Active Device Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Simulate different physical devices writing to and reading from the same Firestore collections:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                syncState.availableDevices.forEach { deviceName ->
                    val isSelected = syncState.activeDeviceName == deviceName
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Emerald500.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) BorderStroke(1.5.dp, Emerald500) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDevice(deviceName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (deviceName.contains("Tab")) Icons.Default.Tablet else Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (isSelected) Emerald600 else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = deviceName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isSelected) "Active Device (Writing & Listening)" else "Tap to switch to this device",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Emerald600 else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Emerald600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
            }
        }
    )
}
