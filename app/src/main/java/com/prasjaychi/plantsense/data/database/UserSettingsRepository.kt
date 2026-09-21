package com.prasjaychi.plantsense.data.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Room database operations for local user settings and preferences.
 */
class UserSettingsRepository(
    private val dao: UserSettingsDao
) {

    val userSettings: Flow<UserSettingsEntity?> = dao.getUserSettings()

    suspend fun getSettings(): UserSettingsEntity = withContext(Dispatchers.IO) {
        dao.getUserSettingsSync() ?: UserSettingsEntity.default()
    }

    suspend fun saveSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateSettings(settings.copy(lastUpdatedMs = System.currentTimeMillis()))
    }

    suspend fun updateMasterReminders(enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.updateMasterReminders(enabled)
    }

    suspend fun updateNotificationTime(hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        dao.updateNotificationTime(hour, minute)
    }

    suspend fun updateTemperatureUnit(unit: String) = withContext(Dispatchers.IO) {
        dao.updateTemperatureUnit(unit)
    }

    suspend fun updateDarkMode(preference: String) = withContext(Dispatchers.IO) {
        dao.updateDarkMode(preference)
    }

    suspend fun updateProfile(name: String, email: String) = withContext(Dispatchers.IO) {
        dao.updateProfile(name, email)
    }

    suspend fun clearSettings() = withContext(Dispatchers.IO) {
        dao.clearSettings()
    }
}
