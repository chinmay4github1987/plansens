package com.prasjaychi.plantsense.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for local Room operations on plant health records.
 */
@Dao
interface PlantHealthRecordDao {

    @Query("SELECT * FROM plant_health_records ORDER BY timestamp_ms DESC")
    fun getAllHealthRecords(): Flow<List<PlantHealthRecordEntity>>

    @Query("SELECT * FROM plant_health_records WHERE plant_id = :plantId ORDER BY timestamp_ms DESC")
    fun getHealthRecordsForPlant(plantId: Long): Flow<List<PlantHealthRecordEntity>>

    @Query("SELECT * FROM plant_health_records WHERE plant_name = :plantName ORDER BY timestamp_ms DESC")
    fun getHealthRecordsForPlantName(plantName: String): Flow<List<PlantHealthRecordEntity>>

    @Query("SELECT * FROM plant_health_records WHERE plant_id = :plantId ORDER BY timestamp_ms DESC LIMIT 1")
    fun getLatestRecordForPlant(plantId: Long): Flow<PlantHealthRecordEntity?>

    @Query("SELECT * FROM plant_health_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): PlantHealthRecordEntity?

    @Query("SELECT AVG(health_score) FROM plant_health_records WHERE plant_id = :plantId")
    fun getAverageHealthScore(plantId: Long): Flow<Float?>

    @Query("SELECT AVG(health_score) FROM plant_health_records WHERE plant_id = :plantId")
    suspend fun getAverageHealthScoreDirect(plantId: Long): Float?

    @Query("SELECT COUNT(*) FROM plant_health_records")
    fun getHealthRecordsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM plant_health_records WHERE plant_id = :plantId")
    fun getHealthRecordsCountForPlant(plantId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PlantHealthRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PlantHealthRecordEntity>)

    @Update
    suspend fun updateRecord(record: PlantHealthRecordEntity)

    @Query("DELETE FROM plant_health_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM plant_health_records WHERE plant_id = :plantId")
    suspend fun deleteRecordsForPlant(plantId: Long)

    @Query("DELETE FROM plant_health_records")
    suspend fun clearAll()
}
