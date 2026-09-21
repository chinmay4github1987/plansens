package com.prasjaychi.plantsense.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager
import java.util.Calendar

/**
 * WorkManager CoroutineWorker that triggers a system local notification when a plant's
 * scheduled watering date is reached, strictly honoring user preferences (master toggle,
 * preferred notification hours, sound/vibration, quiet hours, and repeat cadence).
 */
class PlantWateringReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_PLANT_ID = "key_plant_id"
        const val KEY_PLANT_NAME = "key_plant_name"
        const val KEY_SCIENTIFIC_NAME = "key_scientific_name"
        const val KEY_WATERING_SCHEDULE = "key_watering_schedule"
        const val KEY_SEVERITY = "key_severity"
        const val KEY_HOUR = "key_hour"
        const val KEY_MINUTE = "key_minute"
        const val KEY_REPEAT_DAYS = "key_repeat_days"
        const val KEY_USER_NOTES = "key_user_notes"
        const val KEY_BATTERY_NOT_LOW = "key_battery_not_low"
        const val TAG_PLANT_REMINDER = "plant_watering_reminder"
        private const val TAG = "PlantWateringWorker"
    }

    override suspend fun doWork(): Result {
        val prefsManager = WateringReminderPreferencesManager.getInstance(context)
        val globalPrefs = prefsManager.preferences.value

        // 1. Check master user preference: if master reminders disabled, cancel execution
        if (!globalPrefs.masterRemindersEnabled) {
            Log.d(TAG, "Master reminders disabled by user. Skipping notification.")
            return Result.success()
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // 2. Check quiet hours user preference
        if (globalPrefs.isInQuietHours(currentHour)) {
            Log.d(TAG, "Current time is inside user quiet hours ($currentHour:00). Suppressing audible alert.")
        }

        val plantId = inputData.getLong(KEY_PLANT_ID, 0L)
        var plantName = inputData.getString(KEY_PLANT_NAME) ?: "Plant"
        var scientificName = inputData.getString(KEY_SCIENTIFIC_NAME) ?: ""
        var wateringSchedule = inputData.getString(KEY_WATERING_SCHEDULE) ?: "Check moisture"
        var severity = inputData.getString(KEY_SEVERITY) ?: "MODERATE"
        var targetHour = inputData.getInt(KEY_HOUR, globalPrefs.defaultHour)
        var targetMinute = inputData.getInt(KEY_MINUTE, globalPrefs.defaultMinute)
        var targetRepeatDays = inputData.getInt(KEY_REPEAT_DAYS, globalPrefs.defaultCadenceDays)
        var userNotes = inputData.getString(KEY_USER_NOTES) ?: ""

        // 3. If plantId is valid, verify current entity state in Room DB
        if (plantId > 0L) {
            try {
                val db = PlantDatabase.getDatabase(context)
                val plant = db.plantAnalysisDao().getAnalysisById(plantId)
                if (plant != null) {
                    // If user disabled the reminder in the meantime, abort notification
                    if (!plant.isReminderEnabled) {
                        Log.d(TAG, "Reminder for plant ${plant.commonName} is disabled in database. Aborting.")
                        return Result.success()
                    }
                    plantName = plant.commonName
                    scientificName = plant.scientificName
                    wateringSchedule = plant.wateringSchedule
                    severity = plant.severity
                    if (plant.userNotes.isNotBlank()) {
                        userNotes = plant.userNotes
                    }

                    // Check per-plant reminder entity for specific user preferences
                    val reminderEntity = db.plantReminderDao().getReminderByPlantName(plant.commonName)
                    if (reminderEntity != null) {
                        targetHour = reminderEntity.hour
                        targetMinute = reminderEntity.minute
                        targetRepeatDays = reminderEntity.repeatDays
                    }

                    // Compute next cycle date and schedule subsequent check if enabled
                    val (nextTargetMs, nextFormatted) = PlantWateringWorkScheduler.computeNextWateringDate(
                        plant = plant.copy(lastWateredMs = System.currentTimeMillis()),
                        hour = targetHour,
                        minute = targetMinute,
                        repeatDays = targetRepeatDays
                    )
                    db.plantAnalysisDao().updateReminderStatus(
                        id = plant.id,
                        isEnabled = true,
                        nextWateringMs = nextTargetMs,
                        nextWateringFormatted = nextFormatted
                    )

                    // Re-schedule the recurring WorkManager task based on user preferences
                    PlantWateringWorkScheduler.scheduleWateringWork(
                        context = context,
                        plant = plant.copy(lastWateredMs = System.currentTimeMillis()),
                        hour = targetHour,
                        minute = targetMinute,
                        repeatDays = targetRepeatDays,
                        userNotes = userNotes,
                        requireBatteryNotLow = globalPrefs.requireBatteryNotLow
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking Room database state in worker: ${e.message}")
            }
        }

        // 4. Post system local notification using notification helper with user preference settings
        PlantWateringNotificationHelper.showImmediateWateringNotification(
            context = context,
            plantId = plantId,
            plantName = plantName,
            scientificName = scientificName,
            wateringSchedule = wateringSchedule,
            severity = severity,
            userNotes = userNotes
        )

        return Result.success()
    }
}
