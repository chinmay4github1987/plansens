package com.prasjaychi.plantsense.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prasjaychi.plantsense.MainActivity
import com.prasjaychi.plantsense.R
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import java.util.Calendar

/**
 * Helper object providing local notification channels, scheduling alarms, and dispatching
 * contextual watering reminders derived from plant care protocols.
 */
object PlantWateringNotificationHelper {

    const val CHANNEL_ID = "plant_watering_reminders_channel"
    const val CHANNEL_NAME = "Plant Watering & Care Reminders"
    const val CHANNEL_DESCRIPTION = "Daily reminders and custom schedules for watering indoor plants based on AI diagnosis"

    const val ACTION_PLANT_WATERING_REMINDER = "com.prasjaychi.plantsense.ACTION_PLANT_WATERING_REMINDER"

    const val EXTRA_PLANT_ID = "extra_plant_id"
    const val EXTRA_PLANT_NAME = "extra_plant_name"
    const val EXTRA_SCIENTIFIC_NAME = "extra_scientific_name"
    const val EXTRA_WATERING_INSTRUCTION = "extra_watering_instruction"
    const val EXTRA_SEVERITY = "extra_severity"

    /**
     * Initializes the notification channel on Android Oreo (API 26) and above.
     */
    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setShowBadge(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Throwable) {
            android.util.Log.w("PlantWateringNotification", "Could not create notification channel: ${e.message}")
        }
    }

    /**
     * Extracts an approximate day interval from watering schedule text.
     * E.g. "Every 10-14 days" -> 10 days
     * "Weekly" -> 7 days
     * "Every 3-4 days" -> 3 days
     * "Daily" -> 1 day
     */
    fun parseWateringCadenceDays(scheduleText: String): Int {
        val lower = scheduleText.lowercase()

        // 1. Check multi-number day ranges FIRST: e.g. "10-14 days", "3-4 days", "5-7 days"
        val rangeRegex = Regex("""(\d+)\s*(?:-|to)\s*(\d+)\s*days""")
        val rangeMatch = rangeRegex.find(lower)
        if (rangeMatch != null) {
            val firstNum = rangeMatch.groupValues[1].toIntOrNull()
            if (firstNum != null) return firstNum
        }

        // 2. Match patterns like "every 3-4 weeks" or "3-4 weeks"
        if (lower.contains("3-4 weeks") || lower.contains("3 to 4 weeks") || lower.contains("monthly")) {
            return 21
        }
        if (lower.contains("2-3 weeks") || lower.contains("2 to 3 weeks")) {
            return 14
        }
        if (lower.contains("2 weeks") || lower.contains("biweekly") || lower.contains("14 days")) {
            return 14
        }

        // Match "every X days" or "every X-Y days"
        val everyDaysRegex = Regex("""every\s*(\d+)(?:\s*(?:-|to)\s*\d+)?\s*days""")
        val everyDaysMatch = everyDaysRegex.find(lower)
        if (everyDaysMatch != null) {
            val num = everyDaysMatch.groupValues[1].toIntOrNull()
            if (num != null) return num
        }

        // Match single day indicators
        if (lower.contains("daily") || lower.contains("every day") || lower.contains("once a day")) {
            return 1
        }
        if (lower.contains("alternate day") || lower.contains("every other day")) {
            return 2
        }
        if (lower.contains("twice a week")) {
            return 3
        }
        if (lower.contains("weekly") || lower.contains("once a week") || lower.contains("every week")) {
            return 7
        }

        // Fallback checks for explicit numbers like "every 10 days" or "10-14"
        if (lower.contains("10-14")) return 10
        if (lower.contains("7-10")) return 7

        return 1 // Default to 1 day if not recognized
    }

    /**
     * Schedules a recurring or exact watering reminder for a specific plant entity.
     * @param hour Hour of day (0-23), default 9 AM
     * @param minute Minute (0-59), default 0
     * @param repeatDays Interval in days (e.g. 1 for daily, 7 for weekly)
     */
    fun scheduleWateringReminder(
        context: Context,
        plant: PlantAnalysisEntity,
        hour: Int = 9,
        minute: Int = 0,
        repeatDays: Int = 1
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, PlantReminderReceiver::class.java).apply {
            action = ACTION_PLANT_WATERING_REMINDER
            putExtra(EXTRA_PLANT_ID, plant.id)
            putExtra(EXTRA_PLANT_NAME, plant.commonName)
            putExtra(EXTRA_SCIENTIFIC_NAME, plant.scientificName)
            putExtra(EXTRA_WATERING_INSTRUCTION, plant.wateringSchedule)
            putExtra(EXTRA_SEVERITY, plant.severity)
        }

        val requestCode = (plant.id and 0x7FFF).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intervalMillis = repeatDays * AlarmManager.INTERVAL_DAY

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                intervalMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fallback if exact/inexact permissions differ
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels an existing scheduled reminder for the specified plant.
     */
    fun cancelWateringReminder(context: Context, plantId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PlantReminderReceiver::class.java).apply {
            action = ACTION_PLANT_WATERING_REMINDER
        }
        val requestCode = (plantId and 0x7FFF).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Sends an immediate local notification (also used when user taps "Send Test Reminder" or WorkManager triggers).
     */
    fun showImmediateWateringNotification(
        context: Context,
        plantId: Long,
        plantName: String,
        scientificName: String,
        wateringSchedule: String,
        severity: String,
        userNotes: String = ""
    ) {
        createNotificationChannel(context)

        val prefs = com.prasjaychi.plantsense.data.preferences.WateringReminderPreferencesManager.getInstance(context).preferences.value

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PLANT_ID, plantId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (plantId and 0x7FFF).toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "Water Now"
        val waterNowIntent = Intent(context, PlantWateringActionReceiver::class.java).apply {
            action = PlantWateringActionReceiver.ACTION_WATER_NOW
            putExtra(PlantWateringActionReceiver.EXTRA_PLANT_ID, plantId)
            putExtra(PlantWateringActionReceiver.EXTRA_PLANT_NAME, plantName)
        }
        val waterNowPendingIntent = PendingIntent.getBroadcast(
            context,
            (plantId.toInt() * 10) + 1,
            waterNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "Snooze 1h"
        val snoozeIntent = Intent(context, PlantWateringActionReceiver::class.java).apply {
            action = PlantWateringActionReceiver.ACTION_SNOOZE_1H
            putExtra(PlantWateringActionReceiver.EXTRA_PLANT_ID, plantId)
            putExtra(PlantWateringActionReceiver.EXTRA_PLANT_NAME, plantName)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (plantId.toInt() * 10) + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "🌿 Time to Check & Water: $plantName"
        val noteSuffix = if (userNotes.isNotBlank()) "\n📝 Note: $userNotes" else ""
        val notificationBody = "$scientificName • $wateringSchedule"
        val expandedText = "$notificationBody\n\nCare Status: $severity$noteSuffix\nTap to open PlantSense diagnosis & moisture care plan."

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "💧 Water Now", waterNowPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "⏰ Snooze 1h", snoozePendingIntent)

        if (!prefs.notificationVibration) {
            notificationBuilder.setVibrate(longArrayOf(0))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(plantId.toInt(), notificationBuilder.build())
            }
        } catch (_: SecurityException) {
            // Permission not yet granted
        }
    }
}
