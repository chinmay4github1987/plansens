package com.prasjaychi.plantsense.ui.history

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.notifications.PlantWateringWorkScheduler
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.viewmodel.DiagnosisSeverity
import com.prasjaychi.plantsense.viewmodel.HistoryFilter
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel
import kotlinx.coroutines.launch

/**
 * Screen displaying previous plant analysis results stored in the local Room database.
 */
@Composable
fun PlantHistoryScreen(
    viewModel: PlantHistoryViewModel,
    onSelectAnalysis: (PlantAnalysisEntity) -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToTrends: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var plantForReminderSchedule by remember { mutableStateOf<PlantAnalysisEntity?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    var pendingReminderToggle by remember { mutableStateOf<Pair<PlantAnalysisEntity, Boolean>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingReminderToggle?.let { (plant, enable) ->
            if (isGranted || !enable) {
                viewModel.togglePlantReminder(plant, enable, context)
                coroutineScope.launch {
                    val (_, formattedDate) = PlantWateringWorkScheduler.computeNextWateringDate(plant)
                    snackbarHostState.showSnackbar(
                        if (enable) "Watering alert scheduled for ${plant.commonName} via WorkManager ($formattedDate)"
                        else "Watering alert disabled for ${plant.commonName}"
                    )
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Notification permission is required to deliver watering alerts.")
                }
            }
        }
        pendingReminderToggle = null
    }

    val onTogglePlantReminder: (PlantAnalysisEntity, Boolean) -> Unit = { plant, enable ->
        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                pendingReminderToggle = Pair(plant, true)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.togglePlantReminder(plant, true, context)
                coroutineScope.launch {
                    val (_, formattedDate) = PlantWateringWorkScheduler.computeNextWateringDate(plant)
                    snackbarHostState.showSnackbar(
                        "WorkManager scheduled alert for ${plant.commonName} ($formattedDate)"
                    )
                }
            }
        } else {
            viewModel.togglePlantReminder(plant, enable, context)
            coroutineScope.launch {
                if (enable) {
                    val (_, formattedDate) = PlantWateringWorkScheduler.computeNextWateringDate(plant)
                    snackbarHostState.showSnackbar(
                        "WorkManager scheduled alert for ${plant.commonName} ($formattedDate)"
                    )
                } else {
                    snackbarHostState.showSnackbar("Watering alert disabled for ${plant.commonName}")
                }
            }
        }
    }

    var showGlobalReminderPreferences by remember { mutableStateOf(false) }

    if (showGlobalReminderPreferences) {
        com.prasjaychi.plantsense.ui.preferences.GlobalReminderPreferencesDialog(
            onDismiss = { showGlobalReminderPreferences = false }
        )
    }

    if (showBackupDialog) {
        PlantBackupDialog(
            viewModel = viewModel,
            totalRecords = uiState.totalCount,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (plantForReminderSchedule != null) {
        PlantReminderScheduleDialog(
            plant = plantForReminderSchedule!!,
            onDismiss = { plantForReminderSchedule = null },
            onReminderConfigured = { _ ->
                plantForReminderSchedule = null
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Search Bar Section for Plant Library
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_input"),
            placeholder = { Text("Search by plant name or species (e.g. Monstera, Ficus)...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotEmpty()) Emerald500 else MaterialTheme.colorScheme.outline
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.onSearchQueryChanged("")
                            focusManager.clearFocus()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Emerald500,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Species / Name Filter Pills
        val popularSpecies = listOf("Monstera", "Snake Plant", "Fiddle Leaf", "Pothos", "Calathea", "Peace Lily")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (searchQuery.isEmpty()) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.onSearchQueryChanged("")
                            focusManager.clearFocus()
                        }
                ) {
                    Text(
                        text = "All Plants",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (searchQuery.isEmpty()) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(popularSpecies) { species ->
                val isSelected = searchQuery.equals(species, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Emerald500.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Emerald500) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (isSelected) {
                                viewModel.onSearchQueryChanged("")
                            } else {
                                viewModel.onSearchQueryChanged(species)
                            }
                            focusManager.clearFocus()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFlorist,
                            contentDescription = null,
                            tint = if (isSelected) Emerald500 else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = species,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Search Status Banner
        if (searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Emerald500.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Found ${uiState.items.size} plant(s) matching \"$searchQuery\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald600
                        )
                    }
                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                viewModel.onSearchQueryChanged("")
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Firestore Cross-Device Cloud Sync Card
        FirestoreSyncStatusCard(
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Summary Row
        HistoryMetricsHeader(
            totalCount = uiState.totalCount,
            favoritesCount = uiState.favoritesCount,
            needsCareCount = uiState.needsCareCount,
            onNavigateToTrends = onNavigateToTrends,
            onOpenBackup = { showBackupDialog = true },
            onOpenReminderPreferences = { showGlobalReminderPreferences = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.activeFilter == HistoryFilter.ALL,
                onClick = { viewModel.onFilterChanged(HistoryFilter.ALL) },
                label = { Text("All (${uiState.totalCount})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald500.copy(alpha = 0.15f),
                    selectedLabelColor = Emerald600
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = uiState.activeFilter == HistoryFilter.FAVORITES,
                onClick = { viewModel.onFilterChanged(HistoryFilter.FAVORITES) },
                label = { Text("Saved (${uiState.favoritesCount})", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald500.copy(alpha = 0.15f),
                    selectedLabelColor = Emerald600
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = uiState.activeFilter == HistoryFilter.ATTENTION_NEEDED,
                onClick = { viewModel.onFilterChanged(HistoryFilter.ATTENTION_NEEDED) },
                label = { Text("Needs Care (${uiState.needsCareCount})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    selectedLabelColor = Color(0xFFD97706)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            if (uiState.pendingSyncCount > 0) {
                FilterChip(
                    selected = uiState.activeFilter == HistoryFilter.PENDING_SYNC,
                    onClick = { viewModel.onFilterChanged(HistoryFilter.PENDING_SYNC) },
                    label = { Text("Queued (${uiState.pendingSyncCount})", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List or Empty State
        if (uiState.items.isEmpty()) {
            EmptyHistoryState(
                searchQuery = searchQuery,
                activeFilter = uiState.activeFilter,
                onNavigateToCamera = onNavigateToCamera,
                onResetDemo = { viewModel.resetDemoRecords() },
                onClearSearch = {
                    viewModel.onSearchQueryChanged("")
                    focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = uiState.items,
                    key = { it.id }
                ) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = { onSelectAnalysis(item) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onDelete = { viewModel.deleteAnalysis(item.id) },
                        onScheduleReminder = { plantForReminderSchedule = item },
                        onToggleReminder = { enable -> onTogglePlantReminder(item, enable) },
                        onTestNotification = {
                            viewModel.triggerImmediateTestReminder(item, context)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Dispatched test watering notification for ${item.commonName}")
                            }
                        }
                    )
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    )
}
}

/**
 * Summary metrics strip showing counts, link to D3/Recharts trends visualization, and CSV Database Backup/Restore.
 */
@Composable
private fun HistoryMetricsHeader(
    totalCount: Int,
    favoritesCount: Int,
    needsCareCount: Int,
    onNavigateToTrends: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenReminderPreferences: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadge(
                label = "Total Scans",
                value = totalCount.toString(),
                color = Emerald500,
                modifier = Modifier.weight(1f)
            )
            MetricBadge(
                label = "Bookmarked",
                value = favoritesCount.toString(),
                color = Cyan500,
                modifier = Modifier.weight(1f)
            )
            MetricBadge(
                label = "Under Care",
                value = needsCareCount.toString(),
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // WorkManager Reminder Preferences Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenReminderPreferences() }
                .testTag("reminder_preferences_banner_button"),
            shape = RoundedCornerShape(12.dp),
            color = Cyan500.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Reminders",
                        tint = Cyan500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "WorkManager Reminder Preferences",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan500
                        )
                        Text(
                            text = "Background scheduler, battery saver & preferred times",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Cyan500,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Direct Banner to Trends / Recharts visualization
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTrends() }
                    .testTag("trends_banner_button"),
                shape = RoundedCornerShape(12.dp),
                color = Emerald500.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Trends",
                        tint = Emerald500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trends & Charts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                        Text(
                            text = "Species distribution",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // CSV Backup & Restore button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenBackup() }
                    .testTag("csv_backup_button"),
                shape = RoundedCornerShape(12.dp),
                color = Indigo600.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Indigo600.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "CSV Backup",
                        tint = Indigo600,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CSV Backup & Restore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600
                        )
                        Text(
                            text = "Export & import file",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * High-craft card representing an individual analysis entry stored in Room.
 */
@Composable
private fun HistoryItemCard(
    item: PlantAnalysisEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onScheduleReminder: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onTestNotification: () -> Unit
) {
    val severityColor = when (item.severity) {
        DiagnosisSeverity.OPTIMAL.name -> Emerald500
        DiagnosisSeverity.MILD.name -> Cyan500
        DiagnosisSeverity.MODERATE.name -> Color(0xFFF59E0B)
        DiagnosisSeverity.CRITICAL.name -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Plant species, confidence, bookmark button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = severityColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.severity == DiagnosisSeverity.OPTIMAL.name) Icons.Default.Eco else Icons.Default.LocalFlorist,
                                contentDescription = null,
                                tint = severityColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.commonName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val isSynced = item.syncStatus == "SYNCED"
                            val isPending = item.syncStatus == "PENDING_SYNC" || item.syncStatus == "PENDING_UPDATE"
                            val badgeColor = if (isSynced) Emerald500 else if (isPending) Color(0xFF3B82F6) else Color(0xFF8B5CF6)
                            val badgeLabel = if (isSynced) "Cloud" else if (isPending) "Queued" else "Room"

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSynced) Icons.Default.CloudDone else if (isPending) Icons.Default.CloudQueue else Icons.Default.Storage,
                                        contentDescription = badgeLabel,
                                        tint = badgeColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.scientificName,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onScheduleReminder,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reminder_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Schedule watering reminder",
                            tint = Cyan500,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (item.isFavorite) Emerald500 else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete analysis",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Root Cause Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = severityColor.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.severity == DiagnosisSeverity.OPTIMAL.name) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.primaryCause,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = severityColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // WorkManager Watering Reminder Toggle Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (item.isReminderEnabled) Emerald500.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (item.isReminderEnabled) androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.25f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onScheduleReminder() }
                    .testTag("reminder_card_${item.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (item.isReminderEnabled) Emerald500.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (item.isReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = if (item.isReminderEnabled) "Watering reminder enabled" else "Watering reminder disabled",
                                        tint = if (item.isReminderEnabled) Emerald600 else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Watering Reminder",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isReminderEnabled) Emerald600 else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (item.isReminderEnabled) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (item.isReminderEnabled) "WorkManager" else "Off",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isReminderEnabled) Emerald600 else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                val nextWateringDisplay = if (item.nextWateringFormatted.isNotBlank()) {
                                    item.nextWateringFormatted
                                } else {
                                    PlantWateringWorkScheduler.computeNextWateringDate(item).second
                                }
                                Text(
                                    text = if (item.isReminderEnabled) {
                                        "Scheduled: $nextWateringDisplay"
                                    } else {
                                        "Cadence: ${item.wateringSchedule}"
                                    },
                                    fontSize = 11.sp,
                                    color = if (item.isReminderEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Switch(
                            checked = item.isReminderEnabled,
                            onCheckedChange = { isChecked -> onToggleReminder(isChecked) },
                            modifier = Modifier.testTag("notification_toggle_${item.id}"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Emerald500,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    if (item.isReminderEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Background WorkManager alert active",
                                fontSize = 10.sp,
                                color = Emerald600,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald500.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onTestNotification)
                                    .testTag("test_notification_btn_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Emerald600,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Test Alert",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald600
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vitality Meter & Date Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Vitality Index:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${item.healthScore}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (item.healthScore.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = severityColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                Text(
                    text = item.formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // View Details Action Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Diagnosis & Care Plan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Indigo600,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Empty state component with clear contextual recommendations.
 */
@Composable
private fun EmptyHistoryState(
    searchQuery: String,
    activeFilter: HistoryFilter,
    onNavigateToCamera: () -> Unit,
    onResetDemo: () -> Unit,
    onClearSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (searchQuery.isNotEmpty()) Color(0xFFF59E0B).copy(alpha = 0.12f) else Emerald500.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.History,
                    contentDescription = null,
                    tint = if (searchQuery.isNotEmpty()) Color(0xFFD97706) else Emerald500,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "No Plants Found" else "No Plant History Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) {
                "No saved plants match \"$searchQuery\". Try checking the spelling, or search by species (e.g., Monstera, Ficus, Sansevieria)."
            } else when (activeFilter) {
                HistoryFilter.FAVORITES -> "You haven't bookmarked any plant diagnoses yet."
                HistoryFilter.ATTENTION_NEEDED -> "No plants currently flagged as needing care."
                HistoryFilter.PENDING_SYNC -> "All botanical records are currently synchronized with the cloud."
                HistoryFilter.ALL -> "Capture a plant photo with the camera to generate and store local identification records."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (searchQuery.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Search Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToCamera,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan New Plant", fontSize = 12.sp)
                }
            }
        } else {
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
}
