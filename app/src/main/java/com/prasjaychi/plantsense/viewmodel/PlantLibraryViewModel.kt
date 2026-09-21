package com.prasjaychi.plantsense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prasjaychi.plantsense.data.care.PlantCareTipsProvider
import com.prasjaychi.plantsense.data.care.SpeciesCareGuide
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantAnalysisRepository
import com.prasjaychi.plantsense.data.database.PlantDatabase
import com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class LibraryCategory(val label: String) {
    ALL("All Plants"),
    LOW_LIGHT("Low Light"),
    PET_SAFE("Pet Safe"),
    EASY_CARE("Easy Care"),
    SUCCULENTS("Succulents"),
    AIR_PURIFYING("Air Purifiers")
}

data class PlantLibraryUiState(
    val displayedPlants: List<SpeciesCareGuide> = emptyList(),
    val allPlants: List<SpeciesCareGuide> = emptyList(),
    val activeCategory: LibraryCategory = LibraryCategory.ALL,
    val searchQuery: String = "",
    val selectedPlantDetail: SpeciesCareGuide? = null,
    val savedSpeciesNames: Set<String> = emptySet(),
    val actionMessage: String? = null
)

/**
 * ViewModel managing the Botanical Plant Library, offering encyclopedia search,
 * category filtering, detailed care guidelines, and quick-add to local Room database.
 */
class PlantLibraryViewModel(
    application: Application,
    private val repository: PlantAnalysisRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        PlantAnalysisRepository(
            PlantDatabase.getDatabase(application).plantAnalysisDao(),
            PlantFirestoreSyncManager.getInstance(application)
        )
    )

    private val allCuratedPlants = PlantCareTipsProvider.getCuratedLibrary()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeCategory = MutableStateFlow(LibraryCategory.ALL)
    val activeCategory: StateFlow<LibraryCategory> = _activeCategory

    private val _selectedPlantDetail = MutableStateFlow<SpeciesCareGuide?>(null)
    val selectedPlantDetail: StateFlow<SpeciesCareGuide?> = _selectedPlantDetail

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    val uiState: StateFlow<PlantLibraryUiState> = combine(
        _searchQuery,
        _activeCategory,
        _selectedPlantDetail,
        _actionMessage,
        repository.allAnalyses
    ) { query, category, detail, message, savedEntities ->
        val savedNames = savedEntities.map { it.scientificName.lowercase(Locale.ROOT) }.toSet()

        val filtered = allCuratedPlants.filter { plant ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase(Locale.ROOT)
                plant.commonName.lowercase(Locale.ROOT).contains(q) ||
                        plant.scientificName.lowercase(Locale.ROOT).contains(q) ||
                        plant.family.lowercase(Locale.ROOT).contains(q)
            }

            val matchesCategory = when (category) {
                LibraryCategory.ALL -> true
                LibraryCategory.LOW_LIGHT -> plant.sunlight.lightTolerance.contains("low", ignoreCase = true) ||
                        plant.sunlight.idealPlacement.contains("low", ignoreCase = true) ||
                        plant.scientificName.contains("Sansevieria", ignoreCase = true) ||
                        plant.scientificName.contains("Zamioculcas", ignoreCase = true)
                LibraryCategory.PET_SAFE -> plant.proTips.isPetSafe
                LibraryCategory.EASY_CARE -> plant.watering.cadenceSummary.contains("10") ||
                        plant.watering.cadenceSummary.contains("14") ||
                        plant.scientificName.contains("Sansevieria", ignoreCase = true) ||
                        plant.scientificName.contains("Zamioculcas", ignoreCase = true) ||
                        plant.scientificName.contains("Epipremnum", ignoreCase = true)
                LibraryCategory.SUCCULENTS -> plant.soil.mixName.contains("succulent", ignoreCase = true) ||
                        plant.soil.mixName.contains("cactus", ignoreCase = true) ||
                        plant.scientificName.contains("Aloe", ignoreCase = true) ||
                        plant.scientificName.contains("Crassula", ignoreCase = true) ||
                        plant.scientificName.contains("Sansevieria", ignoreCase = true)
                LibraryCategory.AIR_PURIFYING -> plant.scientificName.contains("Chlorophytum", ignoreCase = true) ||
                        plant.scientificName.contains("Spathiphyllum", ignoreCase = true) ||
                        plant.scientificName.contains("Sansevieria", ignoreCase = true) ||
                        plant.scientificName.contains("Epipremnum", ignoreCase = true)
            }

            matchesQuery && matchesCategory
        }

        PlantLibraryUiState(
            displayedPlants = filtered,
            allPlants = allCuratedPlants,
            activeCategory = category,
            searchQuery = query,
            selectedPlantDetail = detail,
            savedSpeciesNames = savedNames,
            actionMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlantLibraryUiState(allPlants = allCuratedPlants, displayedPlants = allCuratedPlants)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: LibraryCategory) {
        _activeCategory.value = category
    }

    fun onSelectPlantDetail(plant: SpeciesCareGuide?) {
        _selectedPlantDetail.value = plant
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    /**
     * Adds a plant from the library directly into the user's Room database collection.
     */
    fun saveToMyGarden(plant: SpeciesCareGuide) {
        viewModelScope.launch {
            val entity = PlantAnalysisEntity(
                timestampMs = System.currentTimeMillis(),
                scientificName = plant.scientificName,
                commonName = plant.commonName,
                family = plant.family,
                matchConfidence = 99.0f,
                nativeRegion = plant.proTips.botanicalTrivia.take(80),
                leafCharacteristics = plant.proTips.leafMaintenance,
                primaryCause = "Healthy Reference Plant from Library",
                rootCauseCategory = "Optimal",
                severity = "OPTIMAL",
                symptomsJoined = "None • Healthy Specimen",
                pathogenStatus = "Clear",
                physiologicalImpact = "Thriving foliage",
                immediateIntervention = "Follow standard care protocol",
                wateringSchedule = plant.watering.cadenceSummary,
                soilAndRepotting = plant.soil.mixName,
                lightingRecommendation = plant.sunlight.category,
                humidityAndAtmosphere = plant.proTips.humidityRange,
                nutritionCadence = "Monthly balanced aroid/houseplant fertilizer",
                recoveryTimeline = "Ongoing maintenance",
                healthScore = 98,
                isFavorite = true,
                userNotes = "Added from PlantSense Botanical Library"
            )
            repository.saveAnalysis(entity)
            _actionMessage.value = "Saved ${plant.commonName} to your plant collection!"
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlantLibraryViewModel(application) as T
                }
            }
    }
}
