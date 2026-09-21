package com.prasjaychi.plantsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantAnalysisRepository
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager
import com.prasjaychi.plantsense.data.sync.PlantSyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    FAVORITES,
    ATTENTION_NEEDED,
    PENDING_SYNC
}

data class PlantHistoryUiState(
    val items: List<PlantAnalysisEntity> = emptyList(),
    val totalCount: Int = 0,
    val favoritesCount: Int = 0,
    val needsCareCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

/**
 * ViewModel managing Room database history of plant identification and diagnostic analysis,
 * prioritizing local database responsiveness and orchestrating lazy sync with Firestore.
 */
class PlantHistoryViewModel(
    application: Application,
    private val repository: PlantAnalysisRepository,
    private val syncManager: PlantFirestoreSyncManager = PlantFirestoreSyncManager.getInstance(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        PlantAnalysisRepository(
            PlantDatabase.getDatabase(application).plantAnalysisDao(),
            PlantFirestoreSyncManager.getInstance(application)
        ),
        PlantFirestoreSyncManager.getInstance(application)
    )

    val syncState: StateFlow<PlantSyncState> = syncManager.syncState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeFilter = MutableStateFlow(HistoryFilter.ALL)
    val activeFilter: StateFlow<HistoryFilter> = _activeFilter

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            syncManager.startRealtimeSync()
        }
    }

    fun syncWithCloud(forcePull: Boolean = false) {
        syncManager.executeLazySyncDrain(forcePull)
    }

    fun toggleSimulatedNetworkOutage(isOutage: Boolean) {
        syncManager.toggleSimulatedNetworkOutage(isOutage)
    }

    fun simulateRemoteDeviceSync(simulatedPlantName: String = "Fiddle Leaf Fig") {
        syncManager.simulateRemoteDeviceSync(simulatedPlantName)
    }

    fun switchActiveDevice(deviceName: String) {
        syncManager.switchActiveDevice(deviceName)
    }

    fun toggleRealtimeSync() {
        if (syncState.value.isRealtimeActive) {
            syncManager.stopRealtimeSync()
        } else {
            syncManager.startRealtimeSync()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlantHistoryUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allAnalyses
            } else {
                repository.searchAnalyses(query.trim())
            }
        },
        _activeFilter,
        _searchQuery
    ) { rawItems, filter, query ->
        val totalCount = rawItems.size
        val favoritesCount = rawItems.count { it.isFavorite }
        val needsCareCount = rawItems.count { it.severity == "MODERATE" || it.severity == "CRITICAL" }
        val pendingCount = rawItems.count { it.syncStatus != "SYNCED" }

        val filteredItems = when (filter) {
            HistoryFilter.ALL -> rawItems
            HistoryFilter.FAVORITES -> rawItems.filter { it.isFavorite }
            HistoryFilter.ATTENTION_NEEDED -> rawItems.filter {
                it.severity == "MODERATE" || it.severity == "CRITICAL" || it.severity == "MILD"
            }
            HistoryFilter.PENDING_SYNC -> rawItems.filter { it.syncStatus != "SYNCED" }
        }

        PlantHistoryUiState(
            items = filteredItems,
            totalCount = totalCount,
            favoritesCount = favoritesCount,
            needsCareCount = needsCareCount,
            pendingSyncCount = pendingCount,
            activeFilter = filter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlantHistoryUiState(isLoading = true)
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChanged(filter: HistoryFilter) {
        _activeFilter.value = filter
    }

    fun toggleFavorite(item: PlantAnalysisEntity) {
        viewModelScope.launch {
            repository.updateFavorite(item.id, !item.isFavorite)
        }
    }

    /**
     * Toggles WorkManager local notification reminder for a specific plant based on user preferences.
     */
    fun togglePlantReminder(item: PlantAnalysisEntity, isEnabled: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            if (isEnabled) {
                val db = com.prasjaychi.plantsense.data.database.PlantDatabase.getDatabase(context)
                val existing = db.plantReminderDao().getReminderByPlantName(item.commonName)
                val prefs = com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager.getInstance(context).preferences.value
                val hour = existing?.hour ?: prefs.defaultHour
                val minute = existing?.minute ?: prefs.defaultMinute
                val repeatDays = existing?.repeatDays ?: prefs.defaultCadenceDays

                val (targetMs, formattedDate) = com.prasjaychi.plantsense.notifications.PlantWateringWorkScheduler.scheduleWateringWork(
                    context = context,
                    plant = item,
                    hour = hour,
                    minute = minute,
                    repeatDays = repeatDays,
                    userNotes = item.userNotes,
                    requireBatteryNotLow = prefs.requireBatteryNotLow
                )
                repository.updateReminderStatus(
                    id = item.id,
                    isEnabled = true,
                    nextWateringMs = targetMs,
                    nextWateringFormatted = formattedDate
                )
            } else {
                com.prasjaychi.plantsense.notifications.PlantWateringWorkScheduler.cancelWateringWork(
                    context = context,
                    plantId = item.id
                )
                repository.updateReminderStatus(
                    id = item.id,
                    isEnabled = false,
                    nextWateringMs = 0L,
                    nextWateringFormatted = ""
                )
            }
        }
    }

    /**
     * Triggers an immediate test local notification for the plant.
     */
    fun triggerImmediateTestReminder(item: PlantAnalysisEntity, context: android.content.Context) {
        com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper.showImmediateWateringNotification(
            context = context,
            plantId = item.id,
            plantName = item.commonName,
            scientificName = item.scientificName,
            wateringSchedule = item.wateringSchedule,
            severity = item.severity
        )
    }

    fun recordWatering(id: Long, timestampMs: Long, formattedDate: String) {
        viewModelScope.launch {
            repository.updateLastWatered(id, timestampMs, formattedDate)
        }
    }

    /**
     * Records a rapid watering action from the Quick Actions FAB, updating the plant's last watered
     * timestamp and logging a health record into Room database.
     */
    fun quickLogWatering(
        plantId: Long,
        amountPreset: String = "Standard Water (250ml)",
        notes: String = "",
        onCompleted: (() -> Unit)? = null
    ): kotlinx.coroutines.Job = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val formattedDate = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))
        repository.updateLastWatered(plantId, now, formattedDate)
        val plant = repository.getAnalysisById(plantId)
        if (plant != null) {
            val db = PlantDatabase.getDatabase(getApplication())
            val careNote = buildString {
                append("Watered ($amountPreset)")
                if (notes.isNotBlank()) append(" • $notes")
            }
            db.plantHealthRecordDao().insertRecord(
                com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity(
                    plantId = plant.id,
                    plantName = plant.commonName,
                    scientificName = plant.scientificName,
                    timestampMs = now,
                    formattedDate = formattedDate,
                    healthScore = plant.healthScore,
                    vitalityStatus = "HEALTHY",
                    soilMoistureLevel = 0.85f,
                    soilMoistureStatus = "OPTIMAL",
                    careNotes = careNote
                )
            )
        }
        onCompleted?.invoke()
    }

    /**
     * Records a rapid botanical health note from the Quick Actions FAB, persisting it both in
     * the plant's history notes and inserting a new entry into the health records timeline.
     */
    fun quickAddHealthNote(
        plantId: Long,
        noteText: String,
        vitalityStatus: String = "HEALTHY",
        leafCondition: String = "Vibrant",
        onCompleted: (() -> Unit)? = null
    ): kotlinx.coroutines.Job = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val formattedDate = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))
        val plant = repository.getAnalysisById(plantId)
        val plantName = plant?.commonName ?: "Garden Plant"
        val scientificName = plant?.scientificName ?: ""

        if (plant != null) {
            val updatedNotes = if (plant.userNotes.isBlank()) noteText else "${plant.userNotes}\n• $noteText"
            repository.updateUserNotes(plantId, updatedNotes)
        }

        val db = PlantDatabase.getDatabase(getApplication())
        db.plantHealthRecordDao().insertRecord(
            com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity(
                plantId = plantId,
                plantName = plantName,
                scientificName = scientificName,
                timestampMs = now,
                formattedDate = formattedDate,
                healthScore = plant?.healthScore ?: 85,
                vitalityStatus = vitalityStatus,
                leafCondition = leafCondition,
                careNotes = noteText
            )
        )
        onCompleted?.invoke()
    }

    fun deleteAnalysis(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun resetDemoRecords() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.seedInitialDataIfEmpty()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Exports all plant records to a designated user-selected Storage Access Framework URI.
     */
    fun exportToUri(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (com.prasjaychi.plantsense.data.backup.CsvExportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.exportToUri(context, uri)
            onResult(result)
        }
    }

    /**
     * Generates a CSV file in the app's cache directory and creates an ACTION_SEND Share Intent.
     */
    fun exportToShareableFile(
        context: android.content.Context,
        onResult: (com.prasjaychi.plantsense.data.backup.CsvExportResult, android.content.Intent?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.exportToCacheFile(context)
            val intent = if (result.success && result.file != null) {
                com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.createShareIntent(context, result.file)
            } else null
            onResult(result, intent)
        }
    }

    /**
     * Imports plant history records from a selected CSV URI into Room.
     */
    fun importFromUri(
        context: android.content.Context,
        uri: android.net.Uri,
        overwrite: Boolean = false,
        onResult: (com.prasjaychi.plantsense.data.backup.CsvImportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.importFromUri(context, uri, overwrite)
            onResult(result)
        }
    }

    /**
     * Imports plant records directly from raw CSV content string (useful for unit tests and clipboard).
     */
    fun importFromCsvString(
        csvContent: String,
        overwrite: Boolean = false,
        onResult: (com.prasjaychi.plantsense.data.backup.CsvImportResult) -> Unit
    ) {
        viewModelScope.launch {
            val entities = com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.parseCsvStringToEntities(csvContent)
            if (entities.isEmpty()) {
                onResult(
                    com.prasjaychi.plantsense.data.backup.CsvImportResult(
                        success = false,
                        errorMessage = "No valid botanical records found in CSV content."
                    )
                )
            } else {
                val result = repository.importEntities(entities, overwrite)
                onResult(result)
            }
        }
    }

    companion object {
        fun provideFactory(application: Application): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return PlantHistoryViewModel(application) as T
                }
            }
    }
}
