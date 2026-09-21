package com.prasjaychi.plantsense.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on plant analysis history.
 */
@Dao
interface PlantAnalysisDao {

    @Query("SELECT * FROM plant_analysis_history ORDER BY timestamp_ms DESC")
    fun getAllAnalyses(): Flow<List<PlantAnalysisEntity>>

    @Query("SELECT * FROM plant_analysis_history WHERE id = :id LIMIT 1")
    suspend fun getAnalysisById(id: Long): PlantAnalysisEntity?

    @Query("SELECT * FROM plant_analysis_history WHERE is_favorite = 1 ORDER BY timestamp_ms DESC")
    fun getFavoriteAnalyses(): Flow<List<PlantAnalysisEntity>>

    @Query("SELECT * FROM plant_analysis_history WHERE common_name LIKE '%' || :query || '%' OR scientific_name LIKE '%' || :query || '%' OR family LIKE '%' || :query || '%' ORDER BY timestamp_ms DESC")
    fun searchAnalyses(query: String): Flow<List<PlantAnalysisEntity>>

    @Query("SELECT * FROM plant_analysis_history WHERE sync_status != 'SYNCED' ORDER BY timestamp_ms DESC")
    suspend fun getPendingAnalyses(): List<PlantAnalysisEntity>

    @Query("SELECT COUNT(*) FROM plant_analysis_history WHERE sync_status != 'SYNCED'")
    fun getPendingAnalysesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM plant_analysis_history")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT * FROM plant_analysis_history ORDER BY timestamp_ms DESC")
    suspend fun getAllAnalysesList(): List<PlantAnalysisEntity>

    @Query("SELECT * FROM plant_analysis_history WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getAnalysisByRemoteId(remoteId: String): PlantAnalysisEntity?

    @Query("SELECT * FROM plant_analysis_history WHERE common_name = :commonName AND scientific_name = :scientificName LIMIT 1")
    suspend fun getAnalysisByName(commonName: String, scientificName: String): PlantAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: PlantAnalysisEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(analyses: List<PlantAnalysisEntity>)

    @Update
    suspend fun updateAnalysis(analysis: PlantAnalysisEntity)

    @Query("UPDATE plant_analysis_history SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE plant_analysis_history SET last_watered_ms = :timestampMs, last_watered_formatted = :formattedDate WHERE id = :id")
    suspend fun updateLastWatered(id: Long, timestampMs: Long, formattedDate: String)

    @Query("UPDATE plant_analysis_history SET user_notes = :notes WHERE id = :id")
    suspend fun updateUserNotes(id: Long, notes: String)

    @Query("UPDATE plant_analysis_history SET is_reminder_enabled = :isEnabled, next_watering_ms = :nextWateringMs, next_watering_formatted = :nextWateringFormatted WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, isEnabled: Boolean, nextWateringMs: Long, nextWateringFormatted: String)

    @Query("UPDATE plant_analysis_history SET sync_status = :status, last_synced_ms = :timestampMs, remote_id = :remoteId WHERE id = :id")
    suspend fun updateSyncInfo(id: Long, remoteId: String, status: String, timestampMs: Long)

    @Query("DELETE FROM plant_analysis_history WHERE id = :id")
    suspend fun deleteAnalysisById(id: Long)

    @Query("DELETE FROM plant_analysis_history WHERE remote_id = :remoteId")
    suspend fun deleteAnalysisByRemoteId(remoteId: String)

    @Query("DELETE FROM plant_analysis_history")
    suspend fun clearAll()
}
