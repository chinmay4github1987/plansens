package com.prasjaychi.plantsense.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing locally persisted user settings and app preferences.
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = DEFAULT_SETTINGS_ID,

    @ColumnInfo(name = "master_reminders_enabled")
    val masterRemindersEnabled: Boolean = true,

    @ColumnInfo(name = "default_notification_hour")
    val defaultNotificationHour: Int = 9,

    @ColumnInfo(name = "default_notification_minute")
    val defaultNotificationMinute: Int = 0,

    @ColumnInfo(name = "default_cadence_days")
    val defaultCadenceDays: Int = 3,

    @ColumnInfo(name = "require_battery_not_low")
    val requireBatteryNotLow: Boolean = false,

    @ColumnInfo(name = "notification_sound_enabled")
    val notificationSoundEnabled: Boolean = true,

    @ColumnInfo(name = "notification_vibration_enabled")
    val notificationVibrationEnabled: Boolean = true,

    @ColumnInfo(name = "quiet_hours_enabled")
    val quietHoursEnabled: Boolean = true,

    @ColumnInfo(name = "quiet_hours_start_hour")
    val quietHoursStartHour: Int = 22,

    @ColumnInfo(name = "quiet_hours_end_hour")
    val quietHoursEndHour: Int = 7,

    @ColumnInfo(name = "temperature_unit")
    val temperatureUnit: String = "CELSIUS", // CELSIUS, FAHRENHEIT

    @ColumnInfo(name = "dark_mode_preference")
    val darkModePreference: String = "SYSTEM", // SYSTEM, LIGHT, DARK

    @ColumnInfo(name = "auto_sync_enabled")
    val autoSyncEnabled: Boolean = true,

    @ColumnInfo(name = "diagnostic_confidence_threshold")
    val diagnosticConfidenceThreshold: Float = 0.70f,

    @ColumnInfo(name = "user_name")
    val userName: String = "PlantSense Gardener",

    @ColumnInfo(name = "user_email")
    val userEmail: String = "",

    @ColumnInfo(name = "last_updated_ms")
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_SETTINGS_ID = 1

        fun default(): UserSettingsEntity = UserSettingsEntity(id = DEFAULT_SETTINGS_ID)
    }
}
