package com.prasjaychi.plantsense.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for local Room operations on user settings.
 */
@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    fun getUserSettings(id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    suspend fun getUserSettingsSync(id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET master_reminders_enabled = :enabled, last_updated_ms = :timestamp WHERE id = :id")
    suspend fun updateMasterReminders(
        enabled: Boolean,
        id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_settings SET default_notification_hour = :hour, default_notification_minute = :minute, last_updated_ms = :timestamp WHERE id = :id")
    suspend fun updateNotificationTime(
        hour: Int,
        minute: Int,
        id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_settings SET temperature_unit = :unit, last_updated_ms = :timestamp WHERE id = :id")
    suspend fun updateTemperatureUnit(
        unit: String,
        id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_settings SET dark_mode_preference = :mode, last_updated_ms = :timestamp WHERE id = :id")
    suspend fun updateDarkMode(
        mode: String,
        id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_settings SET user_name = :name, user_email = :email, last_updated_ms = :timestamp WHERE id = :id")
    suspend fun updateProfile(
        name: String,
        email: String,
        id: Int = UserSettingsEntity.DEFAULT_SETTINGS_ID,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM user_settings")
    suspend fun clearSettings()
}
