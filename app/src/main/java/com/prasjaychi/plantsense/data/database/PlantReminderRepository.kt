package com.prasjaychi.plantsense.data.database

import android.content.Context
import com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager
import com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper
import com.prasjaychi.plantsense.notifications.PlantWateringWorkScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Repository coordinating local Room persistence, WorkManager background scheduling,
 * and Firestore cross-device synchronization for plant watering reminders based on user preferences.
 */
class PlantReminderRepository(
    private val reminderDao: PlantReminderDao,
    private val plantDao: PlantAnalysisDao
) {
    val allReminders: Flow<List<PlantReminderEntity>> = reminderDao.getAllReminders()

    fun getReminderForPlant(plantId: Long): Flow<PlantReminderEntity?> {
        return reminderDao.getReminderForPlant(plantId)
    }

    /**
     * Schedules a WorkManager background notification based on user preferences,
     * updates Room database state for both PlantReminderEntity and PlantAnalysisEntity,
     * and enqueues Firestore synchronization.
     */
    suspend fun saveReminder(
        context: Context,
        plant: PlantAnalysisEntity,
        hour: Int,
        minute: Int,
        repeatDays: Int,
        userNotes: String = "",
        requireBatteryNotLow: Boolean = false,
        syncManager: PlantFirestoreSyncManager? = null
    ): Long {
        // 1. Schedule background WorkManager unique task based on user preferences
        val (targetMs, formattedDate) = PlantWateringWorkScheduler.scheduleWateringWork(
            context = context,
            plant = plant,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            userNotes = userNotes,
            requireBatteryNotLow = requireBatteryNotLow
        )

        // Cancel any legacy alarm manager reminders for this plant
        PlantWateringNotificationHelper.cancelWateringReminder(context, plant.id)

        // 2. Update PlantAnalysisEntity reminder state
        plantDao.updateReminderStatus(
            id = plant.id,
            isEnabled = true,
            nextWateringMs = targetMs,
            nextWateringFormatted = formattedDate
        )

        // 3. Persist in Room database
        val existing = reminderDao.getReminderByPlantName(plant.commonName)
        val entityToSave = PlantReminderEntity(
            id = existing?.id ?: 0L,
            plantId = plant.id,
            remotePlantId = plant.remoteId,
            plantName = plant.commonName,
            scientificName = plant.scientificName,
            wateringSchedule = plant.wateringSchedule,
            severity = plant.severity,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            isEnabled = true,
            createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis(),
            lastSyncedMs = System.currentTimeMillis(),
            syncStatus = "PENDING_SYNC"
        )

        val rowId = reminderDao.insertReminder(entityToSave)

        // 4. Enqueue non-blocking lazy sync to Firestore
        syncManager?.enqueueLazySync()

        return rowId
    }

    /**
     * Cancels an active WorkManager background watering reminder for the plant,
     * updates Room database state, and enqueues cloud deletion.
     */
    suspend fun cancelReminder(
        context: Context,
        plantId: Long,
        plantName: String,
        syncManager: PlantFirestoreSyncManager? = null
    ) {
        // 1. Cancel WorkManager background task
        PlantWateringWorkScheduler.cancelWateringWork(context, plantId)

        // Also clean up any legacy alarms
        PlantWateringNotificationHelper.cancelWateringReminder(context, plantId)

        // 2. Update PlantAnalysisEntity reminder state in Room
        plantDao.updateReminderStatus(
            id = plantId,
            isEnabled = false,
            nextWateringMs = 0L,
            nextWateringFormatted = ""
        )

        // 3. Room database removal
        val existing = reminderDao.getReminderByPlantName(plantName)
        if (existing != null) {
            reminderDao.deleteReminderById(existing.id)
            if (existing.remoteId.isNotBlank()) {
                syncManager?.enqueueCloudDeletion("REMINDER", existing.remoteId, existing.id)
            } else {
                syncManager?.enqueueLazySync()
            }
        }
    }
}
