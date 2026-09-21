package com.prasjaychi.plantsense.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Broadcast receiver triggered by AlarmManager to post watering notifications on schedule.
 */
class PlantReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PlantWateringNotificationHelper.ACTION_PLANT_WATERING_REMINDER) {
            val plantId = intent.getLongExtra(PlantWateringNotificationHelper.EXTRA_PLANT_ID, 1L)
            val plantName = intent.getStringExtra(PlantWateringNotificationHelper.EXTRA_PLANT_NAME) ?: "Your Plant"
            val scientificName = intent.getStringExtra(PlantWateringNotificationHelper.EXTRA_SCIENTIFIC_NAME) ?: ""
            val wateringSchedule = intent.getStringExtra(PlantWateringNotificationHelper.EXTRA_WATERING_INSTRUCTION)
                ?: "Check soil moisture and water according to care instructions."
            val severity = intent.getStringExtra(PlantWateringNotificationHelper.EXTRA_SEVERITY) ?: "MODERATE"

            PlantWateringNotificationHelper.showImmediateWateringNotification(
                context = context,
                plantId = plantId,
                plantName = plantName,
                scientificName = scientificName,
                wateringSchedule = wateringSchedule,
                severity = severity
            )
        }
    }
}
