package com.prasjaychi.plantsense.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Botanical identification and taxonomy data.
 */
data class PlantIdentification(
    val scientificName: String = "Monstera deliciosa",
    val commonName: String = "Swiss Cheese Plant",
    val family: String = "Araceae",
    val matchConfidence: Float = 98.4f,
    val nativeRegion: String = "Tropical rainforests of Southern Mexico & Panama",
    val leafCharacteristics: String = "Broad cordate leaves with characteristic natural perforations (fenestrations)"
)

/**
 * Root cause etiology and leaf pathology diagnosis.
 */
data class RootCauseDiagnosis(
    val primaryCause: String = "Substrate Compaction & Sub-optimal Drainage",
    val rootCauseCategory: String = "Physiological Stress (Overwatering / Moisture Stagnation)",
    val severity: DiagnosisSeverity = DiagnosisSeverity.MODERATE,
    val symptomsDetected: List<String> = listOf(
        "Early interveinal chlorosis (yellowing) on basal lower foliage",
        "Marginal leaf tip necrosis due to salt accumulation and moisture stress",
        "Substrate moisture retention exceeding 82% at root ball base",
        "Reduced transpiration rate in lower aerial roots"
    ),
    val pathogenStatus: String = "Negative for active fungal blight, bacterial wilt, and spider mite webbing (99.2% confidence)",
    val physiologicalImpact: String = "Restricted root oxygen absorption leading to localized chlorophyll degradation in older leaves."
)

enum class DiagnosisSeverity(val label: String) {
    OPTIMAL("Healthy & Thriving"),
    MILD("Mild Attention Required"),
    MODERATE("Moderate Root Stress Detected"),
    CRITICAL("Severe Intervention Required")
}

/**
 * Prescribed botanical recovery and ongoing maintenance regime.
 */
data class PrescribedCarePlan(
    val immediateIntervention: String = "Halt watering immediately for 6-8 days until moisture in the upper 2.5 inches of substrate drops below 25%.",
    val wateringSchedule: String = "Every 10-14 days; allow the top half of soil to dry out between thorough waterings.",
    val soilAndRepotting: String = "Aerate root zone; repot into an aroid mix containing 40% coarse perlite, 30% pine bark chips, and 30% coco coir with terracotta drainage.",
    val lightingRecommendation: String = "Position in bright, indirect sunlight (approx. 12,000–18,000 lux). Avoid harsh midday sun.",
    val humidityAndAtmosphere: String = "Maintain 55% - 70% relative humidity. Maintain ambient temperature between 18°C–28°C (65°F–82°F).",
    val nutritionCadence: String = "Suspend high-nitrogen liquid fertilizers for 3 weeks; resume with balanced 20-20-20 fertilizer diluted to 25% strength once fresh shoot emerges.",
    val recoveryTimeline: String = "Visible recovery and stabilization expected within 14–21 days.",
    val lastWateredMs: Long = 0L,
    val lastWateredFormatted: String = ""
)

/**
 * Complete diagnosis report.
 */
data class PlantDiagnosisReport(
    val identification: PlantIdentification = PlantIdentification(),
    val rootCause: RootCauseDiagnosis = RootCauseDiagnosis(),
    val carePlan: PrescribedCarePlan = PrescribedCarePlan(),
    val healthScore: Int = 76,
    val timestamp: String = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date()),
    val isSavedToWorkspace: Boolean = false
)

data class PlantAnalysisUiState(
    val capturedBitmap: Bitmap? = null,
    val currentReport: PlantDiagnosisReport = PlantDiagnosisReport(),
    val selectedTab: Int = 0, // 0: Overview & ID, 1: Root Cause Diagnosis, 2: Prescribed Care Plan
    val activeEntityId: Long? = null,
    val isAnalyzing: Boolean = false,
    val isLiveGeminiAi: Boolean = false,
    val aiModelUsed: String = "",
    val analysisError: String? = null
)

class PlantAnalysisViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlantAnalysisUiState())
    val uiState: StateFlow<PlantAnalysisUiState> = _uiState.asStateFlow()

    fun setCapturedBitmap(bitmap: Bitmap?) {
        _uiState.update { it.copy(capturedBitmap = bitmap) }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun loadFromReport(report: PlantDiagnosisReport) {
        _uiState.update {
            it.copy(
                currentReport = report,
                selectedTab = 0,
                activeEntityId = null,
                isAnalyzing = false,
                analysisError = null
            )
        }
    }

    fun loadFromEntity(entity: com.prasjaychi.plantsense.data.database.PlantAnalysisEntity) {
        _uiState.update {
            it.copy(
                currentReport = entity.toReport(),
                selectedTab = 0,
                activeEntityId = entity.id,
                isAnalyzing = false,
                analysisError = null
            )
        }
    }

    /**
     * Executes real-time Gemini AI multimodal botanical diagnosis on the captured photo.
     */
    fun analyzeWithGemini(
        bitmap: Bitmap,
        autoSaveToRoom: Boolean = false,
        repository: com.prasjaychi.plantsense.data.database.PlantAnalysisRepository? = null,
        onComplete: (PlantDiagnosisReport) -> Unit = {}
    ) {
        _uiState.update {
            it.copy(
                capturedBitmap = bitmap,
                isAnalyzing = true,
                analysisError = null
            )
        }

        viewModelScope.launch {
            when (val result = com.prasjaychi.plantsense.data.ai.PlantGeminiAnalyzer.analyzePlantImage(bitmap)) {
                is com.prasjaychi.plantsense.data.ai.GeminiAnalysisResult.Success -> {
                    val reportToUse = result.report
                    _uiState.update {
                        it.copy(
                            currentReport = reportToUse,
                            isAnalyzing = false,
                            isLiveGeminiAi = result.isLiveGeminiCall,
                            aiModelUsed = result.rawModelUsed,
                            analysisError = null
                        )
                    }

                    if (autoSaveToRoom && repository != null) {
                        repository.saveReport(reportToUse)
                    }
                    onComplete(reportToUse)
                }
                is com.prasjaychi.plantsense.data.ai.GeminiAnalysisResult.Error -> {
                    val fallback = result.fallbackReport ?: com.prasjaychi.plantsense.data.ai.PlantGeminiAnalyzer.generateBotanicalHeuristicReport(bitmap)
                    _uiState.update {
                        it.copy(
                            currentReport = fallback,
                            isAnalyzing = false,
                            isLiveGeminiAi = false,
                            aiModelUsed = "Offline Botanical Engine",
                            analysisError = result.errorMessage
                        )
                    }
                    onComplete(fallback)
                }
            }
        }
    }

    fun recordWatering(
        timestampMs: Long,
        formattedDate: String,
        repository: com.prasjaychi.plantsense.data.database.PlantAnalysisRepository? = null
    ) {
        _uiState.update { state ->
            val updatedCarePlan = state.currentReport.carePlan.copy(
                lastWateredMs = timestampMs,
                lastWateredFormatted = formattedDate
            )
            val updatedReport = state.currentReport.copy(carePlan = updatedCarePlan)
            state.copy(currentReport = updatedReport)
        }

        val entityId = _uiState.value.activeEntityId
        if (repository != null && entityId != null && entityId > 0) {
            viewModelScope.launch {
                repository.updateLastWatered(entityId, timestampMs, formattedDate)
            }
        }
    }

    fun toggleSaveReport(repository: com.prasjaychi.plantsense.data.database.PlantAnalysisRepository? = null) {
        val willBeSaved = !_uiState.value.currentReport.isSavedToWorkspace
        _uiState.update { state ->
            val updatedReport = state.currentReport.copy(
                isSavedToWorkspace = willBeSaved
            )
            state.copy(currentReport = updatedReport)
        }

        if (repository != null && willBeSaved) {
            viewModelScope.launch {
                repository.saveReport(_uiState.value.currentReport)
            }
        }
    }

    /**
     * Load preset diagnosis scenarios to test different plant species and root-cause conditions.
     */
    fun loadDiagnosisScenario(scenarioId: String) {
        val (ident, rootCause, carePlan, score) = when (scenarioId) {
            "monstera_overwater" -> Quad(
                PlantIdentification(
                    scientificName = "Monstera deliciosa",
                    commonName = "Swiss Cheese Plant",
                    family = "Araceae",
                    matchConfidence = 98.4f,
                    nativeRegion = "Rainforests of Southern Mexico & Central America",
                    leafCharacteristics = "Fenestrated split-leaf foliage with thick climbing stems"
                ),
                RootCauseDiagnosis(
                    primaryCause = "Substrate Compaction & Sub-optimal Drainage",
                    rootCauseCategory = "Overwatering / Anaerobic Substrate Conditions",
                    severity = DiagnosisSeverity.MODERATE,
                    symptomsDetected = listOf(
                        "Interveinal chlorosis on lower basal leaves",
                        "Marginal tip browning with faint yellow halos",
                        "Dense moisture pockets detected at base of nursery pot"
                    ),
                    pathogenStatus = "Clear of spider mites and powdery mildew",
                    physiologicalImpact = "Hypoxia in fine feeder roots hindering mineral uptake."
                ),
                PrescribedCarePlan(
                    immediateIntervention = "Suspend watering for 7 days. Probe soil with wooden chopstick to aerate.",
                    wateringSchedule = "Every 10-14 days after top 50% of potting volume dries.",
                    soilAndRepotting = "Amend potting substrate with 35% chunky perlite and orchid bark.",
                    lightingRecommendation = "Bright indirect sunlight (12,000–18,000 lux).",
                    humidityAndAtmosphere = "60%+ humidity; avoid cold drafts below 16°C.",
                    nutritionCadence = "Halt feeding until new growth spear develops.",
                    recoveryTimeline = "Foliage stabilization in 2 weeks."
                ),
                76
            )
            "fiddle_oedema" -> Quad(
                PlantIdentification(
                    scientificName = "Ficus lyrata",
                    commonName = "Fiddle-Leaf Fig",
                    family = "Moraceae",
                    matchConfidence = 96.8f,
                    nativeRegion = "Tropical rainforests of Western Africa",
                    leafCharacteristics = "Large lyre-shaped leathery leaves with prominent veining"
                ),
                RootCauseDiagnosis(
                    primaryCause = "Inconsistent Watering Cadence Causing Cellular Oedema",
                    rootCauseCategory = "Hydraulic Pressure Imbalance in Foliar Cells",
                    severity = DiagnosisSeverity.MILD,
                    symptomsDetected = listOf(
                        "Reddish-brown punctate lesions on emerging immature leaves (Oedema)",
                        "Slight downward curling of leaf tips",
                        "Alternating periods of extreme substrate dryness followed by deep saturation"
                    ),
                    pathogenStatus = "No fungal or bacterial spots found; purely physiological",
                    physiologicalImpact = "Rapid water uptake ruptures foliar cell walls before stomata can transpire."
                ),
                PrescribedCarePlan(
                    immediateIntervention = "Establish an unvarying watering cadence rather than erratic soak-and-dry cycles.",
                    wateringSchedule = "Weekly deep watering with complete runoff drain.",
                    soilAndRepotting = "Fast-draining peat, perlite, and coarse sand mix.",
                    lightingRecommendation = "Direct morning sun + continuous bright indirect afternoon light.",
                    humidityAndAtmosphere = "Maintain stable 50-60% humidity; keep away from AC vents.",
                    nutritionCadence = "Apply 3-1-2 NPK fertilizer monthly during active growth seasons.",
                    recoveryTimeline = "New leaves will emerge spotless; existing marks will fade gradually."
                ),
                84
            )
            "snake_healthy" -> Quad(
                PlantIdentification(
                    scientificName = "Sansevieria trifasciata",
                    commonName = "Snake Plant / Mother-in-Law's Tongue",
                    family = "Asparagaceae",
                    matchConfidence = 99.1f,
                    nativeRegion = "Tropical West Africa",
                    leafCharacteristics = "Erect sword-like succulent leaves with gray-green horizontal striping"
                ),
                RootCauseDiagnosis(
                    primaryCause = "Optimal Environmental Equilibrium",
                    rootCauseCategory = "Healthy / Ideal Physiological Function",
                    severity = DiagnosisSeverity.OPTIMAL,
                    symptomsDetected = listOf(
                        "Turgid foliage with rigid cuticle protection",
                        "Uniform chlorophyll saturation across variegated margins",
                        "Well-aerated root zone with no stagnant moisture"
                    ),
                    pathogenStatus = "Completely free of foliar pathogens or root pathogens",
                    physiologicalImpact = "Efficient CAM photosynthesis functioning at maximum efficiency."
                ),
                PrescribedCarePlan(
                    immediateIntervention = "No emergency intervention required. Maintain current care regimen.",
                    wateringSchedule = "Every 3–4 weeks during spring/summer; every 6 weeks in winter.",
                    soilAndRepotting = "Cactus & succulent gritty blend with pumice.",
                    lightingRecommendation = "Tolerates low light, but thrives in moderate to bright indirect light.",
                    humidityAndAtmosphere = "Low to moderate humidity (30-50%); 15°C–30°C.",
                    nutritionCadence = "Diluted succulent fertilizer once in spring and once in mid-summer.",
                    recoveryTimeline = "Currently thriving at peak vigor."
                ),
                98
            )
            else -> Quad(
                PlantIdentification(),
                RootCauseDiagnosis(),
                PrescribedCarePlan(),
                76
            )
        }

        _uiState.update { state ->
            state.copy(
                currentReport = PlantDiagnosisReport(
                    identification = ident,
                    rootCause = rootCause,
                    carePlan = carePlan,
                    healthScore = score,
                    timestamp = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date()),
                    isSavedToWorkspace = false
                )
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
