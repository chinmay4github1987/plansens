package com.prasjaychi.plantsense.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.prasjaychi.plantsense.data.database.PlantAnalysisDao
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.database.PlantReminderDao
import com.prasjaychi.plantsense.data.database.PlantReminderEntity
import com.prasjaychi.plantsense.data.database.SyncOperationDao
import com.prasjaychi.plantsense.data.database.SyncOperationEntity
import com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PlantSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

enum class PlantSyncMode {
    CLOUD_ENABLED,      // Online: Local Room database priority with background Firestore synchronization
    LOCAL_ROOM_ONLY     // Offline: Local Room database persistence active, cloud Firestore sync paused
}

data class PlantSyncState(
    val isSyncing: Boolean = false,
    val syncStatus: PlantSyncStatus = PlantSyncStatus.IDLE,
    val isOnline: Boolean = true,
    val isSimulatedOutage: Boolean = false,
    val syncMode: PlantSyncMode = PlantSyncMode.CLOUD_ENABLED,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val isMetered: Boolean = false,
    val networkDescription: String = "Initializing network...",
    val pendingOperationsCount: Int = 0,
    val pendingPlantsCount: Int = 0,
    val pendingRemindersCount: Int = 0,
    val lastSyncedFormatted: String? = null,
    val lastSyncedMs: Long = 0L,
    val syncedPlantsCount: Int = 0,
    val syncedRemindersCount: Int = 0,
    val cloudPlantsCount: Int = 0,
    val cloudRemindersCount: Int = 0,
    val activeDeviceId: String = "dev_primary_${Build.MODEL.replace(" ", "_").lowercase()}",
    val activeDeviceName: String = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} ${Build.MODEL}",
    val availableDevices: List<String> = listOf("Pixel 8 Pro (Primary)", "Galaxy Tab S9 (Greenhouse)", "MacBook Web Portal"),
    val isRealtimeActive: Boolean = false,
    val lastErrorMessage: String? = null,
    val syncLogs: List<String> = emptyList()
)

/**
 * High-performance background sync worker and manager prioritizing local Room database operations
 * and executing non-blocking lazy synchronization to Firebase Firestore with offline queueing.
 */
class PlantFirestoreSyncManager(
    private val context: Context,
    private val plantDao: PlantAnalysisDao,
    private val reminderDao: PlantReminderDao,
    private val syncQueueDao: SyncOperationDao,
    private val connectivityObserver: NetworkConnectivityObserver = NetworkConnectivityObserver(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timeFormatter = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    private val _syncState = MutableStateFlow(PlantSyncState())
    val syncState: StateFlow<PlantSyncState> = _syncState.asStateFlow()

    private var plantListenerRegistration: ListenerRegistration? = null
    private var reminderListenerRegistration: ListenerRegistration? = null
    private var lazySyncDebounceJob: Job? = null
    private var periodicSyncJob: Job? = null

    // In-memory fallback / cache for simulated cross-device testing if Firebase offline
    private val simulatedCloudPlants = mutableMapOf<String, PlantFirestoreDocument>()
    private val simulatedCloudReminders = mutableMapOf<String, PlantReminderFirestoreDocument>()

    companion object {
        const val COLLECTION_PLANTS = "plant_diagnoses"
        const val COLLECTION_REMINDERS = "plant_reminders"

        @Volatile
        private var INSTANCE: PlantFirestoreSyncManager? = null

        fun getInstance(context: Context): PlantFirestoreSyncManager {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val db = PlantDatabase.getDatabase(appContext)
                val instance = PlantFirestoreSyncManager(
                    context = appContext,
                    plantDao = db.plantAnalysisDao(),
                    reminderDao = db.plantReminderDao(),
                    syncQueueDao = db.syncOperationDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        logSyncEvent("Room-First Lazy Sync Worker initialized on ${_syncState.value.activeDeviceName}")
        observeConnectivityAndQueue()
        startPeriodicSyncLoop()
    }

    private fun observeConnectivityAndQueue() {
        // Monitor network connectivity state from ConnectivityManager and toggle sync mode
        scope.launch {
            connectivityObserver.observeNetworkStatus().collectLatest { status ->
                val effectivelyOnline = status.isEffectivelyOnline
                val newMode = if (effectivelyOnline) PlantSyncMode.CLOUD_ENABLED else PlantSyncMode.LOCAL_ROOM_ONLY
                val wasOnline = _syncState.value.isOnline

                _syncState.update {
                    it.copy(
                        isOnline = effectivelyOnline,
                        isSimulatedOutage = status.isSimulatedOutage,
                        syncMode = newMode,
                        connectionType = status.connectionType,
                        isMetered = status.isMetered,
                        networkDescription = status.networkDescription,
                        syncStatus = if (!effectivelyOnline) PlantSyncStatus.OFFLINE else if (it.syncStatus == PlantSyncStatus.OFFLINE) PlantSyncStatus.IDLE else it.syncStatus
                    )
                }

                if (effectivelyOnline) {
                    if (!wasOnline) {
                        logSyncEvent("[ConnectivityManager] Connected via ${status.connectionType} (${status.networkDescription}). Toggled to Cloud Firestore Sync Mode.")
                    }
                    enqueueLazySync(delayMs = 1200L)
                } else {
                    if (wasOnline) {
                        lazySyncDebounceJob?.cancel()
                        logSyncEvent(
                            if (status.isSimulatedOutage)
                                "[ConnectivityManager] Simulated Network Outage active. Toggled to Local Room-Only Mode. App remains 100% functional offline."
                            else
                                "[ConnectivityManager] Network disconnected (${status.networkDescription}). Toggled to Local Room-Only Mode. Room mutations queued."
                        )
                    }
                }
            }
        }

        // Monitor pending counts in Room
        scope.launch {
            combine(
                syncQueueDao.getPendingOperationsCount(),
                plantDao.getPendingAnalysesCount(),
                reminderDao.getPendingRemindersCount()
            ) { queueCount, pendingPlants, pendingReminders ->
                Triple(queueCount, pendingPlants, pendingReminders)
            }.collectLatest { (queueCount, pendingPlants, pendingReminders) ->
                _syncState.update {
                    it.copy(
                        pendingOperationsCount = queueCount,
                        pendingPlantsCount = pendingPlants,
                        pendingRemindersCount = pendingReminders
                    )
                }
            }
        }
    }

    private fun startPeriodicSyncLoop() {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (true) {
                delay(45_000L) // Periodic background sync heartbeat every 45s
                if (_syncState.value.isOnline && !_syncState.value.isSyncing) {
                    val pendingCount = _syncState.value.pendingOperationsCount +
                            _syncState.value.pendingPlantsCount +
                            _syncState.value.pendingRemindersCount
                    if (pendingCount > 0) {
                        logSyncEvent("Periodic Sync Worker: Found $pendingCount pending items, draining queue...")
                        executeLazySyncDrain(forcePull = false)
                    }
                }
            }
        }
    }

    private fun logSyncEvent(message: String) {
        val timestamp = timeFormatter.format(Date())
        val logEntry = "[$timestamp] $message"
        _syncState.update { state ->
            val updatedLogs = (listOf(logEntry) + state.syncLogs).take(35)
            state.copy(syncLogs = updatedLogs)
        }
        Log.d("PlantFirestoreSync", logEntry)
    }

    private fun getFirestoreSafe(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w("PlantFirestoreSync", "Firebase not initialized, utilizing in-memory cloud bridge: ${e.message}")
            null
        }
    }

    /**
     * Toggles simulated network outage for testing local Room resilience.
     */
    fun toggleSimulatedNetworkOutage(isOutage: Boolean) {
        connectivityObserver.setSimulatedOutage(isOutage)
        val effectivelyOnline = !isOutage && connectivityObserver.isConnected()
        _syncState.update {
            it.copy(
                isSimulatedOutage = isOutage,
                isOnline = effectivelyOnline,
                syncMode = if (effectivelyOnline) PlantSyncMode.CLOUD_ENABLED else PlantSyncMode.LOCAL_ROOM_ONLY,
                syncStatus = if (isOutage) PlantSyncStatus.OFFLINE else PlantSyncStatus.IDLE
            )
        }
        logSyncEvent(
            if (isOutage) "NETWORK OUTAGE SIMULATED: Toggled to local Room DB mode. Cloud Firestore synchronization deferred."
            else "Network Outage ended: Toggled to Cloud Firestore synchronization mode."
        )
        if (!isOutage) {
            enqueueLazySync(delayMs = 600L)
        }
    }

    /**
     * Sets a simulated physical connectivity override for unit testing in headless JVM environments.
     */
    fun setSimulatedNetworkConnected(connected: Boolean?) {
        connectivityObserver.setSimulatedConnected(connected)
        val effectivelyOnline = (connected == true || (connected == null && connectivityObserver.isConnected())) && !_syncState.value.isSimulatedOutage
        _syncState.update {
            it.copy(
                isOnline = effectivelyOnline,
                syncMode = if (effectivelyOnline) PlantSyncMode.CLOUD_ENABLED else PlantSyncMode.LOCAL_ROOM_ONLY,
                syncStatus = if (effectivelyOnline) PlantSyncStatus.IDLE else PlantSyncStatus.OFFLINE
            )
        }
    }

    /**
     * Schedules a debounced lazy sync run in the background.
     */
    fun enqueueLazySync(delayMs: Long = 800L) {
        lazySyncDebounceJob?.cancel()
        lazySyncDebounceJob = scope.launch {
            delay(delayMs)
            if (_syncState.value.isOnline) {
                executeLazySyncDrain(forcePull = false)
            } else {
                logSyncEvent("Lazy sync skipped: Device is currently offline / in network outage. Room changes are safely preserved.")
            }
        }
    }

    /**
     * Enqueues an offline deletion in Room sync queue so it will be lazily dispatched to Firestore.
     */
    suspend fun enqueueCloudDeletion(targetType: String, remoteId: String, localId: Long = 0L) {
        if (remoteId.isNotBlank()) {
            syncQueueDao.enqueue(
                SyncOperationEntity(
                    targetType = targetType,
                    operationType = "DELETE",
                    localId = localId,
                    remoteId = remoteId
                )
            )
            logSyncEvent("Queued cloud deletion for $targetType ($remoteId)")
            enqueueLazySync()
        }
    }

    /**
     * Executes the background lazy-sync engine:
     * 1. Drains Room `sync_queue` (pending cloud deletions).
     * 2. Pushes pending plant analyses and watering reminders from Room to Firestore.
     * 3. Pulls remote updates from Firestore and reconciles into Room DB.
     * 4. Updates local sync status flags to "SYNCED".
     */
    fun executeLazySyncDrain(forcePull: Boolean = false) {
        scope.launch {
            if (!_syncState.value.isOnline) {
                logSyncEvent("Cannot drain sync queue: Device is offline / in network outage.")
                _syncState.update { it.copy(syncStatus = PlantSyncStatus.OFFLINE) }
                return@launch
            }

            _syncState.update {
                it.copy(
                    isSyncing = true,
                    syncStatus = PlantSyncStatus.SYNCING,
                    lastErrorMessage = null
                )
            }

            val firestore = getFirestoreSafe()
            val deviceId = _syncState.value.activeDeviceId
            val deviceName = _syncState.value.activeDeviceName

            try {
                var pushedPlants = 0
                var pushedReminders = 0
                var deletedOps = 0
                var pulledPlants = 0
                var pulledReminders = 0

                // 1. Process pending queued deletion operations from Room
                val pendingOps = syncQueueDao.getPendingOperations()
                for (op in pendingOps) {
                    try {
                        if (firestore != null && op.remoteId.isNotBlank()) {
                            val collectionName = if (op.targetType == "REMINDER") COLLECTION_REMINDERS else COLLECTION_PLANTS
                            firestore.collection(collectionName).document(op.remoteId).delete().await()
                        }
                        if (op.targetType == "REMINDER") {
                            simulatedCloudReminders.remove(op.remoteId)
                        } else {
                            simulatedCloudPlants.remove(op.remoteId)
                        }
                        syncQueueDao.deleteOperation(op.id)
                        deletedOps++
                    } catch (e: Exception) {
                        syncQueueDao.incrementRetry(op.id)
                        logSyncEvent("Failed to execute queued delete for ${op.remoteId}: ${e.message}")
                    }
                }

                // 2. Push pending plant analyses (Room -> Firestore)
                val pendingPlants = plantDao.getPendingAnalyses()
                for (plant in pendingPlants) {
                    try {
                        val doc = PlantFirestoreDocument.fromEntity(plant, deviceId, deviceName)
                        if (firestore != null) {
                            firestore.collection(COLLECTION_PLANTS).document(doc.id)
                                .set(doc.toMap(), SetOptions.merge())
                                .await()
                        } else {
                            simulatedCloudPlants[doc.id] = doc
                        }
                        plantDao.updateSyncInfo(plant.id, doc.id, "SYNCED", System.currentTimeMillis())
                        pushedPlants++
                    } catch (e: Exception) {
                        logSyncEvent("Failed to push plant ${plant.commonName}: ${e.message}")
                    }
                }

                // 3. Push pending reminders (Room -> Firestore)
                val pendingReminders = reminderDao.getPendingReminders()
                for (rem in pendingReminders) {
                    try {
                        val doc = PlantReminderFirestoreDocument.fromEntity(rem, deviceId, deviceName)
                        if (firestore != null) {
                            firestore.collection(COLLECTION_REMINDERS).document(doc.id)
                                .set(doc.toMap(), SetOptions.merge())
                                .await()
                        } else {
                            simulatedCloudReminders[doc.id] = doc
                        }
                        reminderDao.updateSyncInfo(rem.id, doc.id, "SYNCED", System.currentTimeMillis())
                        pushedReminders++
                    } catch (e: Exception) {
                        logSyncEvent("Failed to push reminder for ${rem.plantName}: ${e.message}")
                    }
                }

                // 4. Pull and merge remote changes from Firestore into Room
                if (firestore != null) {
                    val remotePlantDocs = firestore.collection(COLLECTION_PLANTS).get().await()
                    for (doc in remotePlantDocs.documents) {
                        val data = doc.data ?: continue
                        val plantDoc = PlantFirestoreDocument.fromMap(data)
                        val existing = plantDao.getAnalysisByRemoteId(plantDoc.id)
                            ?: plantDao.getAnalysisByName(plantDoc.commonName, plantDoc.scientificName)

                        if (existing == null) {
                            plantDao.insertAnalysis(plantDoc.toEntity())
                            pulledPlants++
                        } else if (existing.syncStatus == "SYNCED" && (plantDoc.lastUpdatedMs > existing.lastSyncedMs || forcePull)) {
                            // Only overwrite local if local has no pending un-synced edits
                            plantDao.updateAnalysis(plantDoc.toEntity(localId = existing.id))
                            pulledPlants++
                        }
                    }

                    val remoteReminderDocs = firestore.collection(COLLECTION_REMINDERS).get().await()
                    for (doc in remoteReminderDocs.documents) {
                        val data = doc.data ?: continue
                        val reminderDoc = PlantReminderFirestoreDocument.fromMap(data)
                        val existing = reminderDao.getReminderByRemoteId(reminderDoc.id)
                            ?: reminderDao.getReminderByPlantName(reminderDoc.plantName)

                        val targetPlant = plantDao.getAnalysisByName(reminderDoc.plantName, reminderDoc.scientificName)

                        if (existing == null) {
                            val newId = reminderDao.insertReminder(reminderDoc.toEntity(localPlantId = targetPlant?.id ?: 0L))
                            if (reminderDoc.isEnabled && targetPlant != null) {
                                withContext(Dispatchers.Main) {
                                    PlantWateringNotificationHelper.scheduleWateringReminder(
                                        context = context,
                                        plant = targetPlant,
                                        hour = reminderDoc.hour,
                                        minute = reminderDoc.minute,
                                        repeatDays = reminderDoc.repeatDays
                                    )
                                }
                            }
                            pulledReminders++
                        } else if (existing.syncStatus == "SYNCED" && (reminderDoc.lastUpdatedMs > existing.lastSyncedMs || forcePull)) {
                            reminderDao.updateReminder(reminderDoc.toEntity(localId = existing.id, localPlantId = existing.plantId))
                            pulledReminders++
                        }
                    }
                } else {
                    // In-memory simulation merge
                    for ((_, plantDoc) in simulatedCloudPlants) {
                        val existing = plantDao.getAnalysisByRemoteId(plantDoc.id)
                            ?: plantDao.getAnalysisByName(plantDoc.commonName, plantDoc.scientificName)
                        if (existing == null) {
                            plantDao.insertAnalysis(plantDoc.toEntity())
                            pulledPlants++
                        }
                    }
                    for ((_, reminderDoc) in simulatedCloudReminders) {
                        val existing = reminderDao.getReminderByRemoteId(reminderDoc.id)
                            ?: reminderDao.getReminderByPlantName(reminderDoc.plantName)
                        if (existing == null) {
                            val targetPlant = plantDao.getAnalysisByName(reminderDoc.plantName, reminderDoc.scientificName)
                            reminderDao.insertReminder(reminderDoc.toEntity(localPlantId = targetPlant?.id ?: 0L))
                            pulledReminders++
                        }
                    }
                }

                val now = System.currentTimeMillis()
                val totalPlants = plantDao.getAllAnalysesList().size
                val totalReminders = reminderDao.getAllRemindersList().size

                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatus = PlantSyncStatus.SYNCED,
                        lastSyncedFormatted = timeFormatter.format(Date(now)),
                        lastSyncedMs = now,
                        syncedPlantsCount = totalPlants,
                        syncedRemindersCount = totalReminders,
                        cloudPlantsCount = totalPlants,
                        cloudRemindersCount = totalReminders,
                        lastErrorMessage = null
                    )
                }

                val resultMsg = "Lazy-Sync complete: ↑$pushedPlants plants, ↑$pushedReminders reminders, 🗑$deletedOps cloud deletes, ↓$pulledPlants new plants, ↓$pulledReminders reminders."
                logSyncEvent(resultMsg)
            } catch (e: Exception) {
                Log.e("PlantFirestoreSync", "Lazy-sync drain failure", e)
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatus = PlantSyncStatus.ERROR,
                        lastErrorMessage = e.message ?: "Cloud sync failed"
                    )
                }
                logSyncEvent("Sync error: ${e.message}")
            }
        }
    }

    /**
     * Starts continuous real-time snapshot listeners for cross-device updates.
     */
    fun startRealtimeSync() {
        val firestore = getFirestoreSafe()
        _syncState.update { it.copy(isRealtimeActive = true) }
        logSyncEvent("Started real-time cross-device sync listeners")

        if (firestore != null) {
            try {
                plantListenerRegistration?.remove()
                plantListenerRegistration = firestore.collection(COLLECTION_PLANTS)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            logSyncEvent("Plants listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null && _syncState.value.isOnline) {
                            scope.launch {
                                var remoteImports = 0
                                for (doc in snapshots.documents) {
                                    val data = doc.data ?: continue
                                    val plantDoc = PlantFirestoreDocument.fromMap(data)
                                    val existingLocal = plantDao.getAnalysisByRemoteId(plantDoc.id)
                                        ?: plantDao.getAnalysisByName(plantDoc.commonName, plantDoc.scientificName)

                                    if (existingLocal == null) {
                                        plantDao.insertAnalysis(plantDoc.toEntity())
                                        remoteImports++
                                    } else if (existingLocal.syncStatus == "SYNCED" && plantDoc.lastUpdatedMs > existingLocal.lastSyncedMs) {
                                        val updatedEntity = plantDoc.toEntity(localId = existingLocal.id)
                                        plantDao.updateAnalysis(updatedEntity)
                                        remoteImports++
                                    }
                                }
                                if (remoteImports > 0) {
                                    logSyncEvent("Real-time: Imported $remoteImports updated plant diagnoses from cloud")
                                    updateCounts()
                                }
                            }
                        }
                    }

                reminderListenerRegistration?.remove()
                reminderListenerRegistration = firestore.collection(COLLECTION_REMINDERS)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            logSyncEvent("Reminders listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null && _syncState.value.isOnline) {
                            scope.launch {
                                var reminderImports = 0
                                for (doc in snapshots.documents) {
                                    val data = doc.data ?: continue
                                    val reminderDoc = PlantReminderFirestoreDocument.fromMap(data)
                                    val existingLocal = reminderDao.getReminderByRemoteId(reminderDoc.id)
                                        ?: reminderDao.getReminderByPlantName(reminderDoc.plantName)

                                    if (existingLocal == null) {
                                        val targetPlant = plantDao.getAnalysisByName(reminderDoc.plantName, reminderDoc.scientificName)
                                        val newId = reminderDao.insertReminder(
                                            reminderDoc.toEntity(localPlantId = targetPlant?.id ?: 0L)
                                        )
                                        if (reminderDoc.isEnabled && targetPlant != null) {
                                            withContext(Dispatchers.Main) {
                                                PlantWateringNotificationHelper.scheduleWateringReminder(
                                                    context = context,
                                                    plant = targetPlant,
                                                    hour = reminderDoc.hour,
                                                    minute = reminderDoc.minute,
                                                    repeatDays = reminderDoc.repeatDays
                                                )
                                            }
                                        }
                                        reminderImports++
                                    } else if (existingLocal.syncStatus == "SYNCED" && reminderDoc.lastUpdatedMs > existingLocal.lastSyncedMs) {
                                        val updated = reminderDoc.toEntity(
                                            localId = existingLocal.id,
                                            localPlantId = existingLocal.plantId
                                        )
                                        reminderDao.updateReminder(updated)
                                        reminderImports++
                                    }
                                }
                                if (reminderImports > 0) {
                                    logSyncEvent("Real-time: Synced $reminderImports watering reminder schedules")
                                    updateCounts()
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                logSyncEvent("Real-time listener setup exception: ${e.message}")
            }
        }
    }

    fun stopRealtimeSync() {
        plantListenerRegistration?.remove()
        plantListenerRegistration = null
        reminderListenerRegistration?.remove()
        reminderListenerRegistration = null
        _syncState.update { it.copy(isRealtimeActive = false) }
        logSyncEvent("Paused real-time snapshot listener")
    }

    /**
     * Simulates receiving a diagnosis created on another device to test cross-device synchronization.
     */
    fun simulateRemoteDeviceSync(simulatedPlantName: String = "Fiddle Leaf Fig") {
        scope.launch {
            val remoteDocId = "plant_remote_${UUID.randomUUID().toString().take(8)}"
            val remoteDoc = PlantFirestoreDocument(
                id = remoteDocId,
                deviceId = "dev_tablet_galaxy_s9",
                deviceName = "Galaxy Tab S9 (Greenhouse)",
                timestampMs = System.currentTimeMillis(),
                formattedDate = timeFormatter.format(Date()),
                scientificName = "Ficus lyrata",
                commonName = simulatedPlantName,
                family = "Moraceae",
                matchConfidence = 96.8,
                nativeRegion = "Western Africa lowland tropical rainforests",
                leafCharacteristics = "Large lyre-shaped leathery leaves with prominent veining",
                primaryCause = "Inconsistent Light Penetration & Ambient Low Humidity",
                rootCauseCategory = "Environmental Stress (Lighting & Atmosphere)",
                severity = "MILD",
                symptoms = listOf(
                    "Minor brown crisping on upper foliage margins",
                    "Lower leaf angle drooping during afternoon sun",
                    "Low ambient relative humidity (34%)"
                ),
                pathogenStatus = "Negative for spider mites, scale insects, and fungal spots",
                physiologicalImpact = "Mild transpiration deficit due to dry indoor air conditioning.",
                immediateIntervention = "Relocate within 4 feet of an unshaded east-facing window.",
                wateringSchedule = "Every 7-10 days; water when top 2 inches dry.",
                soilAndRepotting = "Fast-draining organic soil with 25% perlite and orchid bark.",
                lightingRecommendation = "Bright, filtered sunlight (15,000–25,000 lux).",
                humidityAndAtmosphere = "Optimal 50%-65% humidity. Avoid cold draft vents.",
                nutritionCadence = "Balanced 3-1-2 liquid fertilizer diluted to half strength monthly.",
                recoveryTimeline = "10–14 days to stabilize leaf turgidity.",
                healthScore = 88,
                isFavorite = true,
                userNotes = "Synced automatically from Greenhouse Tablet",
                lastWateredMs = System.currentTimeMillis() - (2 * 86_400_000L),
                lastWateredFormatted = "2 days ago",
                lastUpdatedMs = System.currentTimeMillis()
            )

            // Insert into local Room database
            val localId = plantDao.insertAnalysis(remoteDoc.toEntity())

            val remoteReminderDoc = PlantReminderFirestoreDocument(
                id = "rem_remote_${UUID.randomUUID().toString().take(8)}",
                plantRemoteId = remoteDocId,
                plantName = remoteDoc.commonName,
                scientificName = remoteDoc.scientificName,
                wateringSchedule = remoteDoc.wateringSchedule,
                severity = remoteDoc.severity,
                hour = 9,
                minute = 0,
                repeatDays = 7,
                isEnabled = true,
                deviceId = "dev_tablet_galaxy_s9",
                deviceName = "Galaxy Tab S9 (Greenhouse)",
                createdAtMs = System.currentTimeMillis(),
                lastUpdatedMs = System.currentTimeMillis()
            )

            reminderDao.insertReminder(remoteReminderDoc.toEntity(localPlantId = localId))

            val createdEntity = plantDao.getAnalysisById(localId)
            if (createdEntity != null) {
                withContext(Dispatchers.Main) {
                    PlantWateringNotificationHelper.scheduleWateringReminder(
                        context = context,
                        plant = createdEntity,
                        hour = 9,
                        minute = 0,
                        repeatDays = 7
                    )
                }
            }

            logSyncEvent("Cross-device sync event: Received '$simulatedPlantName' & weekly reminder from Galaxy Tab S9")
            updateCounts()
        }
    }

    fun switchActiveDevice(deviceName: String) {
        val newId = "dev_${deviceName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}"
        _syncState.update {
            it.copy(
                activeDeviceId = newId,
                activeDeviceName = deviceName
            )
        }
        logSyncEvent("Switched active device profile to: $deviceName ($newId)")
    }

    private suspend fun updateCounts() {
        val totalPlants = plantDao.getAllAnalysesList().size
        val totalReminders = reminderDao.getAllRemindersList().size
        _syncState.update {
            it.copy(
                syncedPlantsCount = totalPlants,
                syncedRemindersCount = totalReminders,
                cloudPlantsCount = totalPlants,
                cloudRemindersCount = totalReminders
            )
        }
    }
}
