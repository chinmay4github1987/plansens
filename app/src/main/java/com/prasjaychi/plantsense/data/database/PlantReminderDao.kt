package com.prasjaychi.plantsense.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local plant reminder configurations.
 */
@Dao
interface PlantReminderDao {

    @Query("SELECT * FROM plant_reminders ORDER BY created_at_ms DESC")
    fun getAllReminders(): Flow<List<PlantReminderEntity>>

    @Query("SELECT * FROM plant_reminders ORDER BY created_at_ms DESC")
    suspend fun getAllRemindersList(): List<PlantReminderEntity>

    @Query("SELECT * FROM plant_reminders WHERE sync_status != 'SYNCED' ORDER BY created_at_ms DESC")
    suspend fun getPendingReminders(): List<PlantReminderEntity>

    @Query("SELECT COUNT(*) FROM plant_reminders WHERE sync_status != 'SYNCED'")
    fun getPendingRemindersCount(): Flow<Int>

    @Query("SELECT * FROM plant_reminders WHERE plant_id = :plantId LIMIT 1")
    fun getReminderForPlant(plantId: Long): Flow<PlantReminderEntity?>

    @Query("SELECT * FROM plant_reminders WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getReminderByRemoteId(remoteId: String): PlantReminderEntity?

    @Query("SELECT * FROM plant_reminders WHERE plant_name = :plantName LIMIT 1")
    suspend fun getReminderByPlantName(plantName: String): PlantReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: PlantReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<PlantReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: PlantReminderEntity)

    @Query("UPDATE plant_reminders SET is_enabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE plant_reminders SET sync_status = :status, last_synced_ms = :timestampMs, remote_id = :remoteId WHERE id = :id")
    suspend fun updateSyncInfo(id: Long, remoteId: String, status: String, timestampMs: Long)

    @Query("DELETE FROM plant_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM plant_reminders WHERE plant_id = :plantId")
    suspend fun deleteReminderByPlantId(plantId: Long)

    @Query("DELETE FROM plant_reminders WHERE remote_id = :remoteId")
    suspend fun deleteReminderByRemoteId(remoteId: String)

    @Query("DELETE FROM plant_reminders")
    suspend fun clearAll()
}
