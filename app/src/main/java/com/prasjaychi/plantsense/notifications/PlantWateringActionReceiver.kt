package com.prasjaychi.plantsense.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Broadcast receiver handling interactive actions from plant watering notifications,
 * such as "Water Now" (logs watering and reschedules next WorkManager reminder)
 * and "Snooze 1h" (delays the notification by 1 hour via WorkManager).
 */
class PlantWateringActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WATER_NOW = "com.prasjaychi.plantsense.ACTION_WATER_NOW"
        const val ACTION_SNOOZE_1H = "com.prasjaychi.plantsense.ACTION_SNOOZE_1H"
        const val EXTRA_PLANT_ID = "extra_plant_id"
        const val EXTRA_PLANT_NAME = "extra_plant_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, 0L)
        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME) ?: "Plant"

        // Dismiss the active notification
        if (plantId > 0L) {
            try {
                NotificationManagerCompat.from(context).cancel(plantId.toInt())
            } catch (_: Exception) {}
        }

        when (intent.action) {
            ACTION_WATER_NOW -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = PlantDatabase.getDatabase(context)
                    val plant = db.plantAnalysisDao().getAnalysisById(plantId)
                    val now = System.currentTimeMillis()
                    val formatted = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(now))

                    if (plant != null) {
                        db.plantAnalysisDao().updateLastWatered(plantId, now, formatted)

                        // Read user's reminder preferences
                        val reminderEntity = db.plantReminderDao().getReminderByPlantName(plant.commonName)
                        val prefs = WateringReminderPreferencesManager.getInstance(context).preferences.value
                        val hour = reminderEntity?.hour ?: prefs.defaultHour
                        val minute = reminderEntity?.minute ?: prefs.defaultMinute
                        val repeatDays = reminderEntity?.repeatDays ?: prefs.defaultCadenceDays

                        // Reschedule next WorkManager reminder based on user preferences
                        val (nextTargetMs, nextFormatted) = PlantWateringWorkScheduler.scheduleWateringWork(
                            context = context,
                            plant = plant.copy(lastWateredMs = now),
                            hour = hour,
                            minute = minute,
                            repeatDays = repeatDays,
                            requireBatteryNotLow = prefs.requireBatteryNotLow
                        )

                        db.plantAnalysisDao().updateReminderStatus(
                            id = plantId,
                            isEnabled = true,
                            nextWateringMs = nextTargetMs,
                            nextWateringFormatted = nextFormatted
                        )
                    }

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "💧 Watered $plantName! Next reminder scheduled with WorkManager.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            ACTION_SNOOZE_1H -> {
                // Enqueue a 1-hour delayed WorkManager work request
                val inputData = Data.Builder()
                    .putLong(PlantWateringReminderWorker.KEY_PLANT_ID, plantId)
                    .putString(PlantWateringReminderWorker.KEY_PLANT_NAME, plantName)
                    .build()

                val snoozeWork = OneTimeWorkRequestBuilder<PlantWateringReminderWorker>()
                    .setInitialDelay(1, TimeUnit.HOURS)
                    .setInputData(inputData)
                    .addTag(PlantWateringReminderWorker.TAG_PLANT_REMINDER)
                    .addTag("snooze_$plantId")
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "snooze_work_$plantId",
                    ExistingWorkPolicy.REPLACE,
                    snoozeWork
                )

                Toast.makeText(context, "⏰ Reminder snoozed for 1 hour", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
