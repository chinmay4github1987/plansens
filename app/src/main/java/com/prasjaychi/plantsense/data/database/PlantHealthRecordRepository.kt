package com.prasjaychi.plantsense.data.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Room database operations for plant health records and longitudinal monitoring.
 */
class PlantHealthRecordRepository(
    private val dao: PlantHealthRecordDao
) {

    val allHealthRecords: Flow<List<PlantHealthRecordEntity>> = dao.getAllHealthRecords()
    val totalRecordsCount: Flow<Int> = dao.getHealthRecordsCount()

    fun getHealthRecordsForPlant(plantId: Long): Flow<List<PlantHealthRecordEntity>> {
        return dao.getHealthRecordsForPlant(plantId)
    }

    fun getHealthRecordsForPlantName(plantName: String): Flow<List<PlantHealthRecordEntity>> {
        return dao.getHealthRecordsForPlantName(plantName)
    }

    fun getLatestRecordForPlant(plantId: Long): Flow<PlantHealthRecordEntity?> {
        return dao.getLatestRecordForPlant(plantId)
    }

    fun getAverageHealthScore(plantId: Long): Flow<Float?> {
        return dao.getAverageHealthScore(plantId)
    }

    fun getHealthRecordsCountForPlant(plantId: Long): Flow<Int> {
        return dao.getHealthRecordsCountForPlant(plantId)
    }

    suspend fun getRecordById(id: Long): PlantHealthRecordEntity? = withContext(Dispatchers.IO) {
        dao.getRecordById(id)
    }

    suspend fun insertRecord(record: PlantHealthRecordEntity): Long = withContext(Dispatchers.IO) {
        dao.insertRecord(record)
    }

    suspend fun insertAll(records: List<PlantHealthRecordEntity>) = withContext(Dispatchers.IO) {
        dao.insertAll(records)
    }

    suspend fun updateRecord(record: PlantHealthRecordEntity) = withContext(Dispatchers.IO) {
        dao.updateRecord(record)
    }

    suspend fun deleteRecordById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteRecordById(id)
    }

    suspend fun deleteRecordsForPlant(plantId: Long) = withContext(Dispatchers.IO) {
        dao.deleteRecordsForPlant(plantId)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun logQuickCheckup(
        plantId: Long,
        plantName: String,
        scientificName: String = "",
        healthScore: Int = 100,
        vitalityStatus: String = "HEALTHY",
        soilMoistureLevel: Float = 0.5f,
        soilMoistureStatus: String = "OPTIMAL",
        leafCondition: String = "Vibrant",
        pestPresence: Boolean = false,
        pestDetails: String = "",
        careNotes: String = "",
        fertilizerApplied: Boolean = false,
        prunedOrCleaned: Boolean = false,
        photoUri: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val entity = PlantHealthRecordEntity(
            plantId = plantId,
            plantName = plantName,
            scientificName = scientificName,
            healthScore = healthScore,
            vitalityStatus = vitalityStatus,
            soilMoistureLevel = soilMoistureLevel,
            soilMoistureStatus = soilMoistureStatus,
            leafCondition = leafCondition,
            pestPresence = pestPresence,
            pestDetails = pestDetails,
            careNotes = careNotes,
            fertilizerApplied = fertilizerApplied,
            prunedOrCleaned = prunedOrCleaned,
            photoUri = photoUri
        )
        dao.insertRecord(entity)
    }
}
