package com.prasjaychi.plantsense.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.database.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing user-configured preferences for background watering notifications.
 */
data class WateringReminderPreferences(
    val masterRemindersEnabled: Boolean = true,
    val defaultHour: Int = 9,
    val defaultMinute: Int = 0,
    val defaultCadenceDays: Int = 3,
    val requireBatteryNotLow: Boolean = false,
    val requireCharging: Boolean = false,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22, // 10 PM
    val quietHoursEndHour: Int = 7,    // 7 AM
    val autoRescheduleOnWatering: Boolean = true
) {
    /**
     * Checks whether the given hour falls inside quiet hours.
     */
    fun isInQuietHours(hour: Int): Boolean {
        if (!quietHoursEnabled) return false
        return if (quietHoursStartHour > quietHoursEndHour) {
            // Over midnight (e.g. 22:00 to 07:00)
            hour >= quietHoursStartHour || hour < quietHoursEndHour
        } else {
            hour in quietHoursStartHour until quietHoursEndHour
        }
    }

    /**
     * Formats default preferred time as a readable string, e.g. "09:00 AM".
     */
    fun formattedDefaultTime(): String {
        val amPm = if (defaultHour >= 12) "PM" else "AM"
        val displayHour = when {
            defaultHour == 0 -> 12
            defaultHour > 12 -> defaultHour - 12
            else -> defaultHour
        }
        return String.format(java.util.Locale.US, "%02d:%02d %s", displayHour, defaultMinute, amPm)
    }
}

/**
 * Singleton manager persisting user preferences for plant watering reminders and WorkManager scheduling.
 */
class WateringReminderPreferencesManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<WateringReminderPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): WateringReminderPreferences {
        return WateringReminderPreferences(
            masterRemindersEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true),
            defaultHour = prefs.getInt(KEY_DEFAULT_HOUR, 9),
            defaultMinute = prefs.getInt(KEY_DEFAULT_MINUTE, 0),
            defaultCadenceDays = prefs.getInt(KEY_DEFAULT_CADENCE, 3),
            requireBatteryNotLow = prefs.getBoolean(KEY_REQUIRE_BATTERY_NOT_LOW, false),
            requireCharging = prefs.getBoolean(KEY_REQUIRE_CHARGING, false),
            notificationSound = prefs.getBoolean(KEY_NOTIFICATION_SOUND, true),
            notificationVibration = prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true),
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false),
            quietHoursStartHour = prefs.getInt(KEY_QUIET_START_HOUR, 22),
            quietHoursEndHour = prefs.getInt(KEY_QUIET_END_HOUR, 7),
            autoRescheduleOnWatering = prefs.getBoolean(KEY_AUTO_RESCHEDULE, true)
        )
    }

    fun updateMasterRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        _preferences.value = _preferences.value.copy(masterRemindersEnabled = enabled)
    }

    fun updateDefaultTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DEFAULT_HOUR, hour)
            .putInt(KEY_DEFAULT_MINUTE, minute)
            .apply()
        _preferences.value = _preferences.value.copy(defaultHour = hour, defaultMinute = minute)
    }

    fun updateDefaultCadence(days: Int) {
        prefs.edit().putInt(KEY_DEFAULT_CADENCE, days.coerceAtLeast(1)).apply()
        _preferences.value = _preferences.value.copy(defaultCadenceDays = days.coerceAtLeast(1))
    }

    fun updateBatteryConstraint(requireBatteryNotLow: Boolean) {
        prefs.edit().putBoolean(KEY_REQUIRE_BATTERY_NOT_LOW, requireBatteryNotLow).apply()
        _preferences.value = _preferences.value.copy(requireBatteryNotLow = requireBatteryNotLow)
    }

    fun updateSoundAndVibration(sound: Boolean, vibration: Boolean) {
        prefs.edit()
            .putBoolean(KEY_NOTIFICATION_SOUND, sound)
            .putBoolean(KEY_NOTIFICATION_VIBRATION, vibration)
            .apply()
        _preferences.value = _preferences.value.copy(notificationSound = sound, notificationVibration = vibration)
    }

    fun updateQuietHours(enabled: Boolean, startHour: Int = 22, endHour: Int = 7) {
        prefs.edit()
            .putBoolean(KEY_QUIET_HOURS_ENABLED, enabled)
            .putInt(KEY_QUIET_START_HOUR, startHour)
            .putInt(KEY_QUIET_END_HOUR, endHour)
            .apply()
        _preferences.value = _preferences.value.copy(
            quietHoursEnabled = enabled,
            quietHoursStartHour = startHour,
            quietHoursEndHour = endHour
        )
    }

    fun saveAll(newPreferences: WateringReminderPreferences) {
        prefs.edit()
            .putBoolean(KEY_MASTER_ENABLED, newPreferences.masterRemindersEnabled)
            .putInt(KEY_DEFAULT_HOUR, newPreferences.defaultHour)
            .putInt(KEY_DEFAULT_MINUTE, newPreferences.defaultMinute)
            .putInt(KEY_DEFAULT_CADENCE, newPreferences.defaultCadenceDays)
            .putBoolean(KEY_REQUIRE_BATTERY_NOT_LOW, newPreferences.requireBatteryNotLow)
            .putBoolean(KEY_REQUIRE_CHARGING, newPreferences.requireCharging)
            .putBoolean(KEY_NOTIFICATION_SOUND, newPreferences.notificationSound)
            .putBoolean(KEY_NOTIFICATION_VIBRATION, newPreferences.notificationVibration)
            .putBoolean(KEY_QUIET_HOURS_ENABLED, newPreferences.quietHoursEnabled)
            .putInt(KEY_QUIET_START_HOUR, newPreferences.quietHoursStartHour)
            .putInt(KEY_QUIET_END_HOUR, newPreferences.quietHoursEndHour)
            .putBoolean(KEY_AUTO_RESCHEDULE, newPreferences.autoRescheduleOnWatering)
            .apply()
        _preferences.value = newPreferences

        // Asynchronously persist to Room user_settings table
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PlantDatabase.getDatabase(appContext)
                val current = db.userSettingsDao().getUserSettingsSync() ?: UserSettingsEntity.default()
                db.userSettingsDao().insertOrUpdateSettings(
                    current.copy(
                        masterRemindersEnabled = newPreferences.masterRemindersEnabled,
                        defaultNotificationHour = newPreferences.defaultHour,
                        defaultNotificationMinute = newPreferences.defaultMinute,
                        defaultCadenceDays = newPreferences.defaultCadenceDays,
                        requireBatteryNotLow = newPreferences.requireBatteryNotLow,
                        notificationSoundEnabled = newPreferences.notificationSound,
                        notificationVibrationEnabled = newPreferences.notificationVibration,
                        quietHoursEnabled = newPreferences.quietHoursEnabled,
                        quietHoursStartHour = newPreferences.quietHoursStartHour,
                        quietHoursEndHour = newPreferences.quietHoursEndHour,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val PREFS_NAME = "plantsense_watering_reminder_prefs"
        private const val KEY_MASTER_ENABLED = "master_reminders_enabled"
        private const val KEY_DEFAULT_HOUR = "default_hour"
        private const val KEY_DEFAULT_MINUTE = "default_minute"
        private const val KEY_DEFAULT_CADENCE = "default_cadence"
        private const val KEY_REQUIRE_BATTERY_NOT_LOW = "require_battery_not_low"
        private const val KEY_REQUIRE_CHARGING = "require_charging"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_NOTIFICATION_VIBRATION = "notification_vibration"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        private const val KEY_AUTO_RESCHEDULE = "auto_reschedule_on_watering"

        @Volatile
        private var instance: WateringReminderPreferencesManager? = null

        fun getInstance(context: Context): WateringReminderPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: WateringReminderPreferencesManager(context).also { instance = it }
            }
        }
    }
}
