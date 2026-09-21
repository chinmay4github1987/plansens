package com.prasjaychi.plantsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prasjaychi.plantsense.data.database.PlantAnalysisDao
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.database.PlantHealthRecordDao
import com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class HealthTrendTimeRange(val label: String, val days: Int) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
    ALL_TIME("All", 3650)
}

enum class HealthTrendMetric(val label: String) {
    HEALTH_SCORE("Health Score"),
    SOIL_MOISTURE("Soil Moisture"),
    VITALITY_STATUS("Vitality Status")
}

enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
}

data class PlantFilterOption(
    val plantId: Long?, // null represents "All Plants"
    val displayName: String,
    val scientificName: String = ""
)

data class PlantHealthTrendsUiState(
    val records: List<PlantHealthRecordEntity> = emptyList(),
    val filteredRecords: List<PlantHealthRecordEntity> = emptyList(),
    val availablePlants: List<PlantFilterOption> = listOf(PlantFilterOption(null, "All Plants")),
    val selectedPlantId: Long? = null,
    val selectedTimeRange: HealthTrendTimeRange = HealthTrendTimeRange.THIRTY_DAYS,
    val selectedMetric: HealthTrendMetric = HealthTrendMetric.HEALTH_SCORE,
    val averageHealthScore: Int = 0,
    val latestHealthScore: Int = 0,
    val healthTrendDelta: Int = 0,
    val trendDirection: TrendDirection = TrendDirection.STABLE,
    val vitalityBreakdown: Map<String, Int> = emptyMap(),
    val averageMoisturePercent: Int = 50,
    val totalCheckupsCount: Int = 0,
    val isLoading: Boolean = true
)

class PlantHealthTrendsViewModel(application: Application) : AndroidViewModel(application) {

    private val database: PlantDatabase = PlantDatabase.getDatabase(application)
    private val healthRecordDao: PlantHealthRecordDao = database.plantHealthRecordDao()
    private val analysisDao: PlantAnalysisDao = database.plantAnalysisDao()

    private val _selectedPlantId = MutableStateFlow<Long?>(null)
    private val _selectedTimeRange = MutableStateFlow(HealthTrendTimeRange.THIRTY_DAYS)
    private val _selectedMetric = MutableStateFlow(HealthTrendMetric.HEALTH_SCORE)

    val uiState: StateFlow<PlantHealthTrendsUiState> = combine(
        healthRecordDao.getAllHealthRecords(),
        analysisDao.getAllAnalyses(),
        _selectedPlantId,
        _selectedTimeRange,
        _selectedMetric
    ) { rawHealthRecords, analyses, selectedPlantId, timeRange, metric ->

        // If no health records exist, seed them automatically from analysis history
        val records = if (rawHealthRecords.isEmpty() && analyses.isNotEmpty()) {
            seedRecordsFromAnalyses(analyses)
        } else {
            rawHealthRecords
        }

        // Build list of plant filter options
        val plantOptions = mutableListOf(PlantFilterOption(null, "All Plants"))
        val distinctPlants = analyses.map { PlantFilterOption(it.id, it.commonName, it.scientificName) }
            .distinctBy { it.displayName }
        plantOptions.addAll(distinctPlants)

        // Filter records by selected plant and time range
        val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(timeRange.days.toLong())
        val filtered = records.filter { record ->
            val matchesPlant = selectedPlantId == null || record.plantId == selectedPlantId
            val matchesTime = record.timestampMs >= cutoffMs
            matchesPlant && matchesTime
        }.sortedBy { it.timestampMs }

        val avgScore = if (filtered.isNotEmpty()) {
            filtered.map { it.healthScore }.average().toInt()
        } else 0

        val latestScore = filtered.lastOrNull()?.healthScore ?: 0
        val earliestScore = filtered.firstOrNull()?.healthScore ?: 0
        val scoreDelta = if (filtered.size >= 2) latestScore - earliestScore else 0

        val direction = when {
            scoreDelta > 3 -> TrendDirection.IMPROVING
            scoreDelta < -3 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        val vitalityMap = filtered.groupBy { it.vitalityStatus }
            .mapValues { it.value.size }

        val avgMoisture = if (filtered.isNotEmpty()) {
            (filtered.map { it.soilMoistureLevel }.average() * 100).toInt()
        } else 50

        PlantHealthTrendsUiState(
            records = records,
            filteredRecords = filtered,
            availablePlants = plantOptions,
            selectedPlantId = selectedPlantId,
            selectedTimeRange = timeRange,
            selectedMetric = metric,
            averageHealthScore = avgScore,
            latestHealthScore = latestScore,
            healthTrendDelta = scoreDelta,
            trendDirection = direction,
            vitalityBreakdown = vitalityMap,
            averageMoisturePercent = avgMoisture,
            totalCheckupsCount = records.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlantHealthTrendsUiState(isLoading = true)
    )

    fun selectPlant(plantId: Long?) {
        _selectedPlantId.value = plantId
    }

    fun selectTimeRange(range: HealthTrendTimeRange) {
        _selectedTimeRange.value = range
    }

    fun selectMetric(metric: HealthTrendMetric) {
        _selectedMetric.value = metric
    }

    fun logHealthCheckup(
        plantId: Long,
        plantName: String,
        scientificName: String = "",
        healthScore: Int,
        vitalityStatus: String,
        soilMoistureLevel: Float,
        soilMoistureStatus: String,
        leafCondition: String,
        careNotes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = PlantHealthRecordEntity(
                plantId = plantId,
                plantName = plantName,
                scientificName = scientificName,
                timestampMs = System.currentTimeMillis(),
                formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date()),
                healthScore = healthScore.coerceIn(0, 100),
                vitalityStatus = vitalityStatus,
                soilMoistureLevel = soilMoistureLevel.coerceIn(0f, 1f),
                soilMoistureStatus = soilMoistureStatus,
                leafCondition = leafCondition,
                careNotes = careNotes
            )
            healthRecordDao.insertRecord(record)
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            healthRecordDao.deleteRecordById(recordId)
        }
    }

    private suspend fun seedRecordsFromAnalyses(
        analyses: List<com.prasjaychi.plantsense.data.database.PlantAnalysisEntity>
    ): List<PlantHealthRecordEntity> = withContext(Dispatchers.IO) {
        val seeded = mutableListOf<PlantHealthRecordEntity>()
        val now = System.currentTimeMillis()

        analyses.forEachIndexed { plantIdx, plant ->
            // Create 4-5 historical progression checkpoints across past 28 days
            val baseScore = plant.healthScore
            val daysAgoList = listOf(24, 18, 12, 6, 1)

            daysAgoList.forEachIndexed { checkIdx, daysAgo ->
                val recordTime = now - TimeUnit.DAYS.toMillis(daysAgo.toLong())
                // Simulate progressive recovery towards current score
                val progressiveScore = (baseScore - (daysAgoList.size - 1 - checkIdx) * 4).coerceIn(40, 100)
                val status = when {
                    progressiveScore >= 85 -> "THRIVING"
                    progressiveScore >= 70 -> "HEALTHY"
                    progressiveScore >= 50 -> "NEEDS_ATTENTION"
                    else -> "CRITICAL"
                }
                val moisture = (0.35f + (checkIdx % 3) * 0.2f).coerceIn(0.2f, 0.9f)
                val moistureText = when {
                    moisture > 0.7f -> "WET"
                    moisture > 0.4f -> "OPTIMAL"
                    else -> "DRY"
                }

                val entity = PlantHealthRecordEntity(
                    plantId = plant.id,
                    plantName = plant.commonName,
                    scientificName = plant.scientificName,
                    timestampMs = recordTime,
                    formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(recordTime)),
                    healthScore = progressiveScore,
                    vitalityStatus = status,
                    soilMoistureLevel = moisture,
                    soilMoistureStatus = moistureText,
                    leafCondition = if (progressiveScore > 75) "Vibrant, upright" else "Minor yellowing tips",
                    careNotes = "Routine inspection. Soil moisture checked."
                )
                seeded.add(entity)
            }
        }

        if (seeded.isNotEmpty()) {
            healthRecordDao.insertAll(seeded)
        }
        seeded
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return PlantHealthTrendsViewModel(application) as T
                }
            }
    }
}
