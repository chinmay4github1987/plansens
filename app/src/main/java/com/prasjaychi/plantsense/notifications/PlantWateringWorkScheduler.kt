package com.prasjaychi.plantsense.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Helper object providing WorkManager scheduling, constraints management,
 * and status observation for plant watering reminders based on user preferences.
 */
object PlantWateringWorkScheduler {

    const val WORK_NAME_PREFIX = "plant_watering_work_"

    fun getWorkName(plantId: Long): String = "$WORK_NAME_PREFIX$plantId"

    /**
     * Computes the target timestamp (ms) and formatted date string for the next watering.
     * Respects user's preferred notification hour, minute, and repeat cadence in days.
     */
    fun computeNextWateringDate(
        plant: PlantAnalysisEntity,
        hour: Int = 9,
        minute: Int = 0,
        repeatDays: Int? = null
    ): Pair<Long, String> {
        val cadenceDays = repeatDays
            ?: PlantWateringNotificationHelper.parseWateringCadenceDays(plant.wateringSchedule).coerceAtLeast(1)
        val baseMs = if (plant.lastWateredMs > 0L) plant.lastWateredMs else plant.timestampMs
        val targetDateMs = baseMs + (cadenceDays * 24L * 60L * 60L * 1000L)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = targetDateMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        // If the calculated time has already elapsed, advance to the next upcoming occurrence
        // at the user's preferred time (today if hour:minute is in future, otherwise tomorrow)
        if (calendar.timeInMillis <= now) {
            val nextOccurrence = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            calendar.timeInMillis = nextOccurrence.timeInMillis
        }

        val targetCalMs = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("MMM dd, yyyy '•' hh:mm a", Locale.getDefault())
        val formatted = dateFormat.format(Date(targetCalMs))
        return Pair(targetCalMs, formatted)
    }

    /**
     * Schedules a unique OneTimeWorkRequest using WorkManager that triggers based on
     * user preferences (preferred notification time, cadence, battery constraints).
     */
    fun scheduleWateringWork(
        context: Context,
        plant: PlantAnalysisEntity,
        hour: Int? = null,
        minute: Int? = null,
        repeatDays: Int? = null,
        userNotes: String? = null,
        requireBatteryNotLow: Boolean? = null
    ): Pair<Long, String> {
        val prefs = WateringReminderPreferencesManager.getInstance(context).preferences.value

        val targetHour = hour ?: prefs.defaultHour
        val targetMinute = minute ?: prefs.defaultMinute
        val targetRepeatDays = repeatDays
            ?: PlantWateringNotificationHelper.parseWateringCadenceDays(plant.wateringSchedule).coerceAtLeast(1)
        val batteryConstraint = requireBatteryNotLow ?: prefs.requireBatteryNotLow

        val (targetMs, formattedDate) = computeNextWateringDate(
            plant = plant,
            hour = targetHour,
            minute = targetMinute,
            repeatDays = targetRepeatDays
        )

        val now = System.currentTimeMillis()
        val delayMs = (targetMs - now).coerceAtLeast(2_000L)

        // Configure WorkManager constraints according to user preferences
        val constraints = Constraints.Builder().apply {
            if (batteryConstraint) {
                setRequiresBatteryNotLow(true)
            }
            if (prefs.requireCharging) {
                setRequiresCharging(true)
            }
        }.build()

        val inputData = Data.Builder()
            .putLong(PlantWateringReminderWorker.KEY_PLANT_ID, plant.id)
            .putString(PlantWateringReminderWorker.KEY_PLANT_NAME, plant.commonName)
            .putString(PlantWateringReminderWorker.KEY_SCIENTIFIC_NAME, plant.scientificName)
            .putString(PlantWateringReminderWorker.KEY_WATERING_SCHEDULE, plant.wateringSchedule)
            .putString(PlantWateringReminderWorker.KEY_SEVERITY, plant.severity)
            .putInt(PlantWateringReminderWorker.KEY_HOUR, targetHour)
            .putInt(PlantWateringReminderWorker.KEY_MINUTE, targetMinute)
            .putInt(PlantWateringReminderWorker.KEY_REPEAT_DAYS, targetRepeatDays)
            .putString(PlantWateringReminderWorker.KEY_USER_NOTES, userNotes ?: plant.userNotes)
            .putBoolean(PlantWateringReminderWorker.KEY_BATTERY_NOT_LOW, batteryConstraint)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PlantWateringReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(PlantWateringReminderWorker.TAG_PLANT_REMINDER)
            .addTag("plant_${plant.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkName(plant.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return Pair(targetMs, formattedDate)
    }

    /**
     * Cancels an active WorkManager watering reminder task for the specified plant.
     */
    fun cancelWateringWork(context: Context, plantId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkName(plantId))
    }

    /**
     * Cancels all scheduled WorkManager watering reminder background jobs.
     */
    fun cancelAllWateringWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(PlantWateringReminderWorker.TAG_PLANT_REMINDER)
    }

    /**
     * Reschedules all active reminders in the Room database using the updated user preferences.
     */
    suspend fun rescheduleAllActiveReminders(context: Context) {
        withContext(Dispatchers.IO) {
            val db = PlantDatabase.getDatabase(context)
            val allPlants = db.plantAnalysisDao().getAllAnalysesList()
            val reminderPrefs = WateringReminderPreferencesManager.getInstance(context).preferences.value

            if (!reminderPrefs.masterRemindersEnabled) {
                cancelAllWateringWork(context)
                return@withContext
            }

            for (plant in allPlants) {
                if (plant.isReminderEnabled) {
                    val reminderEntity = db.plantReminderDao().getReminderByPlantName(plant.commonName)
                    val hour = reminderEntity?.hour ?: reminderPrefs.defaultHour
                    val minute = reminderEntity?.minute ?: reminderPrefs.defaultMinute
                    val repeatDays = reminderEntity?.repeatDays ?: reminderPrefs.defaultCadenceDays

                    val (targetMs, formattedDate) = scheduleWateringWork(
                        context = context,
                        plant = plant,
                        hour = hour,
                        minute = minute,
                        repeatDays = repeatDays,
                        userNotes = plant.userNotes,
                        requireBatteryNotLow = reminderPrefs.requireBatteryNotLow
                    )

                    db.plantAnalysisDao().updateReminderStatus(
                        id = plant.id,
                        isEnabled = true,
                        nextWateringMs = targetMs,
                        nextWateringFormatted = formattedDate
                    )
                }
            }
        }
    }

    /**
     * Observes whether a WorkManager task is enqueued or running for a specific plant.
     */
    fun observeWorkStatus(context: Context, plantId: Long): Flow<Boolean> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(getWorkName(plantId))
            .map { list ->
                list.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            }
    }

    /**
     * Observes the detailed WorkInfo state for a specific plant.
     */
    fun observeWorkState(context: Context, plantId: Long): Flow<WorkInfo.State?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(getWorkName(plantId))
            .map { list ->
                list.firstOrNull()?.state
            }
    }

    /**
     * Observes the total number of actively enqueued or running WorkManager plant reminders.
     */
    fun observeActiveRemindersCount(context: Context): Flow<Int> {
        return WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(PlantWateringReminderWorker.TAG_PLANT_REMINDER)
            .map { list ->
                list.count { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            }
    }
}
