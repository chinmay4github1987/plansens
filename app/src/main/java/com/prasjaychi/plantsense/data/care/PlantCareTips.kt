package com.prasjaychi.plantsense.data.care

import com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport
import java.util.Locale

/**
 * Individual soil component formulation descriptor.
 */
data class SubstrateComponent(
    val name: String,
    val percentage: Int,
    val colorHex: String,
    val purpose: String
)

/**
 * Contextual Sunlight & Lighting advice tailored to the botanical species.
 */
data class SunlightCareAdvice(
    val category: String,
    val luxRange: String,
    val footCandles: String,
    val idealPlacement: String,
    val windowOrientation: String,
    val dailyExposureHours: String,
    val lightTolerance: String,
    val lightStressSigns: String
)

/**
 * Contextual Soil, Substrate & Repotting advice tailored to the botanical species.
 */
data class SoilCareAdvice(
    val mixName: String,
    val optimalPh: String,
    val drainageLevel: String,
    val components: List<SubstrateComponent>,
    val repottingFrequency: String,
    val potRecommendation: String,
    val rootAerationTip: String
)

/**
 * Contextual Watering & Moisture advice tailored to the botanical species.
 */
data class WateringCareAdvice(
    val cadenceSummary: String,
    val moistureCheckMethod: String,
    val targetDrynessPercentage: String,
    val wateringTechnique: String,
    val waterQualityRecommendation: String,
    val summerVsWinterAdjust: String,
    val overwateringSigns: String,
    val underwateringSigns: String
)

/**
 * Contextual species trivia, toxicity, humidity and foliage maintenance pro-tips.
 */
data class BotanicalProTip(
    val humidityRange: String,
    val temperatureRange: String,
    val petSafetyStatus: String,
    val isPetSafe: Boolean,
    val toxicityDetails: String,
    val leafMaintenance: String,
    val seasonalGrowthHabit: String,
    val botanicalTrivia: String
)

/**
 * Comprehensive contextual care guide for a plant species.
 */
data class SpeciesCareGuide(
    val scientificName: String,
    val commonName: String,
    val family: String,
    val sunlight: SunlightCareAdvice,
    val soil: SoilCareAdvice,
    val watering: WateringCareAdvice,
    val proTips: BotanicalProTip
)

/**
 * Botanical care knowledge provider that maps identified species to detailed contextual care advice.
 */
object PlantCareTipsProvider {

    fun getCareGuideFor(report: PlantDiagnosisReport): SpeciesCareGuide {
        val scientific = report.identification.scientificName.trim()
        val common = report.identification.commonName.trim()
        val family = report.identification.family.trim()
        val liveCare = report.carePlan

        return findExactOrFamilyMatch(scientific, common, family, liveCare)
    }

    private fun findExactOrFamilyMatch(
        scientific: String,
        common: String,
        family: String,
        liveCare: com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan
    ): SpeciesCareGuide {
        val s = scientific.lowercase(Locale.ROOT)
        val c = common.lowercase(Locale.ROOT)
        val f = family.lowercase(Locale.ROOT)

        return when {
            // Monstera deliciosa / Adansonii
            s.contains("monstera") || c.contains("swiss cheese") || c.contains("monstera") -> {
                SpeciesCareGuide(
                    scientificName = "Monstera deliciosa",
                    commonName = "Swiss Cheese Plant",
                    family = "Araceae",
                    sunlight = SunlightCareAdvice(
                        category = "Bright Indirect Sunlight",
                        luxRange = "10,000 – 20,000 Lux",
                        footCandles = "1,000 – 1,800 FC",
                        idealPlacement = "1–2 meters from an East-facing window or filtered South/West light.",
                        windowOrientation = "East / Filtered West Window",
                        dailyExposureHours = "8–10 hours daily",
                        lightTolerance = "Tolerates moderate indirect light; lower light produces fewer leaf fenestrations.",
                        lightStressSigns = "Sunburn: Pale bleaching and crisp brown scorch patches. Low light: Slow growth, small solid leaves with no splits."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Chunky Aerated Aroid Mix",
                        optimalPh = "5.5 – 6.5 (Slightly Acidic)",
                        drainageLevel = "High Porosity (Rapid Drainage)",
                        components = listOf(
                            SubstrateComponent("Orchid Pine Bark", 35, "#854D0E", "Coarse aeration & root grip"),
                            SubstrateComponent("Coarse Perlite / Pumice", 30, "#94A3B8", "Prevents compaction"),
                            SubstrateComponent("Coco Coir & Peat", 25, "#78350F", "Moisture retention"),
                            SubstrateComponent("Worm Castings", 10, "#1E293B", "Organic trace minerals")
                        ),
                        repottingFrequency = "Every 18–24 months in spring when aerial roots outgrow container.",
                        potRecommendation = "Terracotta or heavy nursery pot with wide base to support climbing weight.",
                        rootAerationTip = "Guide mature aerial roots into moss pole or back down into the potting mix."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 10–14 days (dry top 50%)",
                        moistureCheckMethod = "Insert finger or wooden moisture probe 2–3 inches deep. Soil should feel dry before rehydrating.",
                        targetDrynessPercentage = "50% – 60% Substrate Dryout",
                        wateringTechnique = "Drench thoroughly until water runs freely through bottom drainage holes. Discard pooled saucer water.",
                        waterQualityRecommendation = "Room-temperature filtered water or tap water left out for 24h to dissipate chlorine.",
                        summerVsWinterAdjust = "Summer: Hydrate every 7–10 days. Winter: Reduce frequency to every 14–20 days as growth slows.",
                        overwateringSigns = "Lower basal leaves turning yellow with faint brown halos, mushy stems.",
                        underwateringSigns = "Curling droopy leaves, dry crispy tips, soil pulling away from pot edges."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "60% – 80% Relative Humidity",
                        temperatureRange = "18°C – 30°C (65°F – 86°F) • Avoid drafts < 15°C",
                        petSafetyStatus = "Toxic to Cats & Dogs (Insoluble Calcium Oxalates)",
                        isPetSafe = false,
                        toxicityDetails = "Foliage contains insoluble calcium oxalate raphide crystals causing oral irritation and drooling if ingested.",
                        leafMaintenance = "Gently wipe large leaves with a damp microfiber cloth monthly to clear dust and boost chlorophyll absorption.",
                        seasonalGrowthHabit = "Vigorous climbing vine in spring and summer; produces aerial roots for moss pole climbing.",
                        botanicalTrivia = "In tropical rainforest canopies, Monstera leaves naturally evolve fenestrations to let hurricane winds and dappled canopy sun pass through."
                    )
                )
            }

            // Ficus lyrata / Fiddle-Leaf Fig
            s.contains("ficus lyrata") || c.contains("fiddle") -> {
                SpeciesCareGuide(
                    scientificName = "Ficus lyrata",
                    commonName = "Fiddle-Leaf Fig",
                    family = "Moraceae",
                    sunlight = SunlightCareAdvice(
                        category = "Bright Direct / Filtered High Light",
                        luxRange = "15,000 – 30,000 Lux",
                        footCandles = "1,500 – 3,000 FC",
                        idealPlacement = "Directly in an East or South-facing window with 2–4 hours of gentle direct sun.",
                        windowOrientation = "Direct East / South Window",
                        dailyExposureHours = "8–12 hours daily",
                        lightTolerance = "Poor low light tolerance. Insufficient light leads to severe lower leaf dropping.",
                        lightStressSigns = "Low light: Dropping healthy green leaves. Excessive heat: Scalded brown blotches on upper canopy."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Fast-Draining Peat & Perlite Loam",
                        optimalPh = "6.0 – 7.0 (Near Neutral)",
                        drainageLevel = "Moderate to High Porosity",
                        components = listOf(
                            SubstrateComponent("Quality Potting Loam", 50, "#78350F", "Stable nutrient structure"),
                            SubstrateComponent("Perlite / Pumice", 30, "#94A3B8", "Drainage channel aeration"),
                            SubstrateComponent("Pine Bark Fines", 20, "#854D0E", "Organic porosity")
                        ),
                        repottingFrequency = "Every 2–3 years in late spring. Ficus prefers being slightly snug in its pot.",
                        potRecommendation = "Heavy ceramic or sturdy nursery container with central drainage.",
                        rootAerationTip = "Rotate plant 90° every 2 weeks so foliage grows evenly towards light source."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 7–10 days (top 2 inches dry)",
                        moistureCheckMethod = "Stick a wooden skewer 2 inches into soil; water only when the skewer comes out clean and dry.",
                        targetDrynessPercentage = "Top 40% – 50% Substrate Dry",
                        wateringTechnique = "Deep soaking with lukewarm water until drainage runs clear. Never allow pot to stand in standing water.",
                        waterQualityRecommendation = "Prefers filtered or distilled water. High mineral tap water may cause brown crusting.",
                        summerVsWinterAdjust = "Summer: Weekly hydration during active flushes. Winter: Stretch to every 12–16 days.",
                        overwateringSigns = "Tiny red/brown spots on new leaves (Oedema), yellowing and leaf drop from the bottom.",
                        underwateringSigns = "Entire canopy droops softly; new leaves emerge crinkled and stunted."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "50% – 65% Humidity",
                        temperatureRange = "18°C – 28°C (65°F – 82°F) • Sensitive to cold drafts",
                        petSafetyStatus = "Mildly Toxic to Pets (Irritant Latex Sap)",
                        isPetSafe = false,
                        toxicityDetails = "Stems exude a milky white latex sap containing furocoumarins that can irritate pet skin and oral membranes.",
                        leafMaintenance = "Shake stem gently for 1 minute weekly to simulate wind, stimulating lignin production and thicker trunk growth.",
                        seasonalGrowthHabit = "Pushes explosive leaf flushes in spring and mid-summer in groups of 2–4 large lyre leaves.",
                        botanicalTrivia = "In its native West African rainforest habitat, Fiddle-Leaf Figs start life as epiphytes high in tree forks before rooting into the ground."
                    )
                )
            }

            // Sansevieria / Snake Plant
            s.contains("sansevieria") || s.contains("dracaena trifasciata") || c.contains("snake plant") || c.contains("mother-in-law") -> {
                SpeciesCareGuide(
                    scientificName = "Dracaena trifasciata (Sansevieria)",
                    commonName = "Snake Plant",
                    family = "Asparagaceae",
                    sunlight = SunlightCareAdvice(
                        category = "Adaptable (Low to Bright Direct)",
                        luxRange = "2,000 – 15,000 Lux",
                        footCandles = "200 – 1,500 FC",
                        idealPlacement = "Any room location from dim corners to sunny windowsills.",
                        windowOrientation = "North, East, or Filtered South",
                        dailyExposureHours = "6–10 hours adaptable",
                        lightTolerance = "Extremely shade tolerant. Thrives faster with bright indirect light.",
                        lightStressSigns = "Very low light: Foliage becomes darker and growth stalls. Excessive sun: Bleached washed-out variegation."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Gritty Cactus & Succulent Mineral Blend",
                        optimalPh = "6.0 – 7.5 (Neutral to slightly alkaline)",
                        drainageLevel = "Very High Drainage (Zero Stagnation)",
                        components = listOf(
                            SubstrateComponent("Coarse Sand & Pumice", 45, "#94A3B8", "Maximum mineral drainage"),
                            SubstrateComponent("Succulent Peat Mix", 35, "#78350F", "Light organic buffer"),
                            SubstrateComponent("Perlite", 20, "#CBD5E1", "Anti-compaction aeration")
                        ),
                        repottingFrequency = "Rarely (Every 3–5 years). Snake plants thrive when rootbound in tight pots.",
                        potRecommendation = "Unglazed Terracotta pot with bottom drainage hole to wick moisture.",
                        rootAerationTip = "Use heavy shallow pots to prevent top-heavy vertical blades from tipping over."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 2–4 weeks (100% dry soil)",
                        moistureCheckMethod = "Ensure the soil is completely dry all the way to the bottom of the pot before adding water.",
                        targetDrynessPercentage = "90% – 100% Complete Dryout",
                        wateringTechnique = "Bottom-watering or careful perimeter watering. Avoid pouring water directly into the central leaf rosette.",
                        waterQualityRecommendation = "Tolerates standard tap water well.",
                        summerVsWinterAdjust = "Summer: Water every 2–3 weeks. Winter: Water once every 6–8 weeks (or suspend if kept cool).",
                        overwateringSigns = "Squishy mushy base, leaves collapsing sideways, foul smell at root ball.",
                        underwateringSigns = "Wrinkled vertical ridges on leaves, tip browning."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "30% – 50% Low / Standard Room Humidity",
                        temperatureRange = "15°C – 32°C (59°F – 90°F) • Extremely hardy",
                        petSafetyStatus = "Mildly Toxic (Saponins)",
                        isPetSafe = false,
                        toxicityDetails = "Contains saponin compounds that cause mild gastrointestinal upset in cats and dogs if chewed.",
                        leafMaintenance = "Dust upright leaves with dry cloth; avoid leaf shine sprays which clog CAM stomata.",
                        seasonalGrowthHabit = "Underground rhizomes push up new pup shoots during warm summer months.",
                        botanicalTrivia = "Snake plants utilize Crassulacean Acid Metabolism (CAM) to absorb CO2 and release oxygen at night, making them ideal bedroom plants."
                    )
                )
            }

            // Epipremnum aureum / Pothos
            s.contains("epipremnum") || c.contains("pothos") || c.contains("devil's ivy") -> {
                SpeciesCareGuide(
                    scientificName = "Epipremnum aureum",
                    commonName = "Golden Pothos",
                    family = "Araceae",
                    sunlight = SunlightCareAdvice(
                        category = "Moderate to Bright Indirect",
                        luxRange = "5,000 – 15,000 Lux",
                        footCandles = "500 – 1,500 FC",
                        idealPlacement = "Bookshelf, hanging basket, or desk 1–3 meters from light source.",
                        windowOrientation = "North or East-facing Room",
                        dailyExposureHours = "8–10 hours daily",
                        lightTolerance = "Highly adaptable; survives low light but may revert variegated yellow streaks to solid green.",
                        lightStressSigns = "Direct sun: Leaves turn yellow with crisp bleached patches. Low light: Loss of golden variegation."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Standard Aerated Houseplant Mix",
                        optimalPh = "6.1 – 6.5 (Slightly Acidic)",
                        drainageLevel = "Moderate Drainage",
                        components = listOf(
                            SubstrateComponent("Indoor Potting Mix", 60, "#78350F", "Nutrient-rich base"),
                            SubstrateComponent("Perlite", 25, "#94A3B8", "Root oxygenation"),
                            SubstrateComponent("Orchid Bark", 15, "#854D0E", "Porosity & moisture buffer")
                        ),
                        repottingFrequency = "Every 1–2 years when roots emerge from drainage holes.",
                        potRecommendation = "Hanging basket or pot with drainage saucer; easy to prune and propagate in water.",
                        rootAerationTip = "Trim trailing vines above a leaf node to promote bushier growth from the crown."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 7–10 days (dry top 2 inches)",
                        moistureCheckMethod = "Touch top 2 inches of soil; water when it feels completely dry and pot feels lightweight.",
                        targetDrynessPercentage = "50% – 70% Substrate Dryout",
                        wateringTechnique = "Water thoroughly until drainage occurs. Pothos will give a slight visual wilt when ready for water.",
                        waterQualityRecommendation = "Standard tap water is well tolerated.",
                        summerVsWinterAdjust = "Summer: Water weekly. Winter: Water every 12–14 days.",
                        overwateringSigns = "Yellow leaves with black stem bases and fungal gnats.",
                        underwateringSigns = "Limp, wilted trailing foliage that rebounds within hours of watering."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "45% – 70% Moderate Humidity",
                        temperatureRange = "17°C – 30°C (62°F – 86°F)",
                        petSafetyStatus = "Toxic to Pets (Calcium Oxalate Crystals)",
                        isPetSafe = false,
                        toxicityDetails = "Insoluble calcium oxalates can cause mouth irritation and vomiting if ingested by pets.",
                        leafMaintenance = "Routinely pinch back vine tips to stimulate branching along the main stem.",
                        seasonalGrowthHabit = "Fast trailing climber that can grow over 10 feet indoors in a single season.",
                        botanicalTrivia = "Golden Pothos is nearly impossible to kill and has been shown to help filter indoor volatile airborne compounds."
                    )
                )
            }

            // Spathiphyllum / Peace Lily
            s.contains("spathiphyllum") || c.contains("peace lily") -> {
                SpeciesCareGuide(
                    scientificName = "Spathiphyllum wallisii",
                    commonName = "Peace Lily",
                    family = "Araceae",
                    sunlight = SunlightCareAdvice(
                        category = "Low to Medium Indirect Light",
                        luxRange = "3,000 – 10,000 Lux",
                        footCandles = "300 – 1,000 FC",
                        idealPlacement = "Shaded corners, office spaces, or North-facing windows.",
                        windowOrientation = "North Window or Interior Room",
                        dailyExposureHours = "6–8 hours indirect light",
                        lightTolerance = "Thrives in low light, but brighter indirect light encourages frequent white spathe blooms.",
                        lightStressSigns = "Direct sun: Leaves quickly scorch and turn black. Deep shade: Foliage thrives but plant will not bloom."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Moisture-Retentive Peat & Loam Blend",
                        optimalPh = "5.8 – 6.5 (Slightly Acidic)",
                        drainageLevel = "Evenly Moist with Free Drainage",
                        components = listOf(
                            SubstrateComponent("Peat Moss / Coco Coir", 50, "#78350F", "Moisture retention"),
                            SubstrateComponent("Perlite", 30, "#94A3B8", "Prevents compaction"),
                            SubstrateComponent("Composted Bark", 20, "#854D0E", "Root stability")
                        ),
                        repottingFrequency = "Every 1–2 years in early spring; can be divided into multiple plants.",
                        potRecommendation = "Glazed ceramic or plastic pot with drainage hole to retain even humidity.",
                        rootAerationTip = "Keep soil consistently damp like a wrung-out sponge, never waterlogged."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 5–7 days (top 1 inch dry)",
                        moistureCheckMethod = "Check top 1 inch of soil. Water as soon as top feels slightly dry or plant begins subtle droop.",
                        targetDrynessPercentage = "25% – 35% Dryout (Never allow complete dryout)",
                        wateringTechnique = "Generous watering with room-temperature water. Peace Lilies dramatically wilt when thirsty.",
                        waterQualityRecommendation = "Highly sensitive to fluoride and chlorine in tap water; use filtered water to prevent brown leaf tips.",
                        summerVsWinterAdjust = "Summer: Water twice weekly if warm. Winter: Water once weekly.",
                        overwateringSigns = "Black leaf tips with yellow margins, root rot.",
                        underwateringSigns = "Dramatic total collapse/drooping of all stems (rebounds within 2 hours of hydration)."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "55% – 80% High Humidity Preferred",
                        temperatureRange = "18°C – 26°C (65°F – 80°F) • Sensitive to cold < 13°C",
                        petSafetyStatus = "Toxic to Pets (Calcium Oxalates)",
                        isPetSafe = false,
                        toxicityDetails = "True lilies are deadly to cats, but Peace Lily is an Araceae which causes oral irritation and drooling, not kidney failure.",
                        leafMaintenance = "Snip dead brown flower spathes at the soil line to stimulate new bloom cycles.",
                        seasonalGrowthHabit = "Evergreen clumping habit; blooms spring and autumn with showy white modified leaves (spathes).",
                        botanicalTrivia = "Peace Lilies communicate their hydration status dramatically by drooping their entire foliage when thirsty, making them great moisture gauges."
                    )
                )
            }

            // Calathea / Maranta / Goeppertia (Prayer Plants)
            s.contains("calathea") || s.contains("maranta") || s.contains("goeppertia") || c.contains("prayer plant") || c.contains("calathea") -> {
                SpeciesCareGuide(
                    scientificName = "Goeppertia (Calathea) ornata",
                    commonName = "Pin-Stripe Prayer Plant",
                    family = "Marantaceae",
                    sunlight = SunlightCareAdvice(
                        category = "Medium Dappled Indirect Light",
                        luxRange = "4,000 – 12,000 Lux",
                        footCandles = "400 – 1,200 FC",
                        idealPlacement = "Set back 2 meters from an East window or protected under sheer curtains.",
                        windowOrientation = "East / North Filtered",
                        dailyExposureHours = "8–10 hours soft light",
                        lightTolerance = "Cannot tolerate any direct sunlight. Foliage patterns will bleach and curl tightly under bright rays.",
                        lightStressSigns = "Direct sun: Crisp curling edges and washed out pink stripes. Low light: Slow growth and muted burgundy undersides."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Fluffy Humus & Perlite Aerated Mix",
                        optimalPh = "6.0 – 6.5 (Slightly Acidic)",
                        drainageLevel = "Consistently Moist & Free-Draining",
                        components = listOf(
                            SubstrateComponent("Coco Coir & Peat", 50, "#78350F", "Uniform moisture"),
                            SubstrateComponent("Perlite", 25, "#94A3B8", "Porosity"),
                            SubstrateComponent("Orchid Bark", 15, "#854D0E", "Root aeration"),
                            SubstrateComponent("Worm Castings", 10, "#1E293B", "Gentle nutrition")
                        ),
                        repottingFrequency = "Every 12–18 months in spring. Use shallow wide pots.",
                        potRecommendation = "Plastic container inside decorative pot to conserve root zone humidity.",
                        rootAerationTip = "Keep substrate consistently moist without allowing root ball to sit in stagnant runoff."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 5–8 days (top 1 inch dry)",
                        moistureCheckMethod = "Touch topsoil; water when the top 1 inch is dry. Substrate should never dry out completely.",
                        targetDrynessPercentage = "20% – 30% Surface Dryout",
                        wateringTechnique = "Hydrate evenly from top or bottom until damp. Empty drip tray after 15 minutes.",
                        waterQualityRecommendation = "Distilled, Reverse Osmosis, or Rainwater ONLY. Tap water minerals cause immediate crispy brown margins.",
                        summerVsWinterAdjust = "Summer: Water every 4–6 days. Winter: Water every 7–10 days.",
                        overwateringSigns = "Yellow translucent leaves, fungus gnats, root rot.",
                        underwateringSigns = "Leaves curling tightly into cigars, crispy brown outer perimeter."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "65% – 85% High Relative Humidity",
                        temperatureRange = "18°C – 27°C (65°F – 80°F) • Never place near radiators or AC",
                        petSafetyStatus = "100% Non-Toxic & Pet Safe! (ASPCA Certified)",
                        isPetSafe = true,
                        toxicityDetails = "Completely safe and non-toxic to cats, dogs, and humans.",
                        leafMaintenance = "Use a room humidifier rather than misting, as standing water droplets on leaves can invite fungal spotting.",
                        seasonalGrowthHabit = "Practices nyctinasty: leaves fold upright at night like hands in prayer and lower during the day.",
                        botanicalTrivia = "The dynamic folding motion is controlled by microscopic hydraulic joints (pulvini) at the base of each leaf stem."
                    )
                )
            }

            // ZZ Plant / Zamioculcas zamiifolia
            s.contains("zamioculcas") || c.contains("zz plant") || c.contains("zanzibar") -> {
                SpeciesCareGuide(
                    scientificName = "Zamioculcas zamiifolia",
                    commonName = "ZZ Plant",
                    family = "Araceae",
                    sunlight = SunlightCareAdvice(
                        category = "Low to Bright Indirect Light",
                        luxRange = "1,500 – 15,000 Lux",
                        footCandles = "150 – 1,500 FC",
                        idealPlacement = "Virtually anywhere indoors from windowless offices to sunny living rooms.",
                        windowOrientation = "North, East, or Windowless Office",
                        dailyExposureHours = "6–10 hours adaptable",
                        lightTolerance = "Unrivaled shade tolerance; tolerates fluorescent office lighting indefinitely.",
                        lightStressSigns = "Direct sun: Foliage bleaches and develops scorched patches."
                    ),
                    soil = SoilCareAdvice(
                        mixName = "Gritty Well-Draining Succulent Mix",
                        optimalPh = "6.0 – 7.0 (Neutral)",
                        drainageLevel = "High Drainage & Aeration",
                        components = listOf(
                            SubstrateComponent("Coarse Perlite & Pumice", 40, "#94A3B8", "Anti-compaction"),
                            SubstrateComponent("Quality Potting Soil", 40, "#78350F", "Nutrient support"),
                            SubstrateComponent("Pine Bark", 20, "#854D0E", "Porosity")
                        ),
                        repottingFrequency = "Every 2–3 years when potato-like underground rhizomes warp the pot.",
                        potRecommendation = "Heavy ceramic pot with large drainage hole.",
                        rootAerationTip = "Water stored in underground tubers allows the plant to survive months of drought."
                    ),
                    watering = WateringCareAdvice(
                        cadenceSummary = if (liveCare.wateringSchedule.isNotBlank()) liveCare.wateringSchedule else "Every 3–4 weeks (100% dry soil)",
                        moistureCheckMethod = "Insert wooden chopstick to the bottom of the pot; water only when 100% bone dry.",
                        targetDrynessPercentage = "90% – 100% Complete Dryout",
                        wateringTechnique = "Soak thoroughly and let drain. Overwatering is the single way to kill a ZZ plant.",
                        waterQualityRecommendation = "Standard tap water is fine.",
                        summerVsWinterAdjust = "Summer: Water every 3 weeks. Winter: Water once every 6–8 weeks.",
                        overwateringSigns = "Stems turn yellow and mushy at the base, leaflets drop off easily.",
                        underwateringSigns = "Leaflets wrinkle slightly and lower leaflets drop."
                    ),
                    proTips = BotanicalProTip(
                        humidityRange = "30% – 50% Standard Ambient Humidity",
                        temperatureRange = "15°C – 30°C (60°F – 86°F)",
                        petSafetyStatus = "Toxic to Pets (Calcium Oxalates)",
                        isPetSafe = false,
                        toxicityDetails = "Contains calcium oxalate crystals; keep away from curious pets and toddlers.",
                        leafMaintenance = "Naturally glossy cuticle requires no leaf shine oils; wipe with a dry microfiber cloth.",
                        seasonalGrowthHabit = "Produces new upright shoots from rhizomes that unfurl bright lime green.",
                        botanicalTrivia = "ZZ plants store water in thick potato-like underground rhizomes, allowing them to survive extreme dry seasons in East Africa."
                    )
                )
            }

            // Dynamic Species Fallback & Synthesis from Gemini Data
            else -> {
                synthesizeDynamicCareGuide(scientific, common, family, liveCare)
            }
        }
    }

    private fun synthesizeDynamicCareGuide(
        scientific: String,
        common: String,
        family: String,
        liveCare: com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan?
    ): SpeciesCareGuide {
        val sci = if (scientific.isNotBlank()) scientific else "Botanical Specimen"
        val com = if (common.isNotBlank()) common else "Identified Species"
        val fam = if (family.isNotBlank()) family else "Plantae"

        val lightDesc = liveCare?.lightingRecommendation ?: "Bright indirect sunlight (8,000–18,000 Lux)"
        val soilDesc = liveCare?.soilAndRepotting ?: "Well-draining potting substrate with 30% perlite"
        val waterDesc = liveCare?.wateringSchedule ?: "Water when top 1-2 inches of soil are dry"
        val humidDesc = liveCare?.humidityAndAtmosphere ?: "Maintain 50%–70% relative humidity and 18°C–26°C"

        val isSucculentOrCactus = fam.contains("cact", true) || fam.contains("crassul", true) || fam.contains("asparag", true) || com.contains("succulent", true) || com.contains("cactus", true)
        val isAroid = fam.contains("arace", true) || com.contains("philodendron", true) || com.contains("alocasia", true)

        val sunlight = SunlightCareAdvice(
            category = if (isSucculentOrCactus) "Direct / High Sunlight" else "Bright Indirect Sunlight",
            luxRange = if (isSucculentOrCactus) "20,000 – 40,000 Lux" else "10,000 – 22,000 Lux",
            footCandles = if (isSucculentOrCactus) "2,000 – 4,000 FC" else "1,000 – 2,200 FC",
            idealPlacement = if (isSucculentOrCactus) "Directly on a South or West facing windowsill" else "1–2 meters from an East-facing window with filtered rays",
            windowOrientation = if (isSucculentOrCactus) "South / West Window" else "East / Filtered West Window",
            dailyExposureHours = "8–10 hours daily",
            lightTolerance = "Thrives in $lightDesc",
            lightStressSigns = "Look for leaf browning, yellowing margins, or leggy stems if lighting is unbalanced."
        )

        val soil = SoilCareAdvice(
            mixName = if (isSucculentOrCactus) "Gritty Mineral Succulent Blend" else if (isAroid) "Aerated Chunky Aroid Substrate" else "Premium Aerated Houseplant Soil",
            optimalPh = "6.0 – 6.8 (Slightly Acidic to Neutral)",
            drainageLevel = if (isSucculentOrCactus) "Rapid Mineral Drainage" else "High Porosity Drainage",
            components = if (isSucculentOrCactus) listOf(
                SubstrateComponent("Pumice & Coarse Sand", 50, "#94A3B8", "Fast mineral drainage"),
                SubstrateComponent("Succulent Peat Loam", 30, "#78350F", "Nutrient matrix"),
                SubstrateComponent("Perlite", 20, "#CBD5E1", "Aeration")
            ) else listOf(
                SubstrateComponent("Quality Potting Mix", 50, "#78350F", "Nutrient matrix"),
                SubstrateComponent("Coarse Perlite", 30, "#94A3B8", "Root oxygenation"),
                SubstrateComponent("Orchid Pine Bark", 20, "#854D0E", "Drainage channels")
            ),
            repottingFrequency = "Every 12–24 months in spring when roots begin coiling in container.",
            potRecommendation = "Pot with adequate drainage holes to prevent anaerobic moisture stagnation.",
            rootAerationTip = soilDesc
        )

        val watering = WateringCareAdvice(
            cadenceSummary = waterDesc,
            moistureCheckMethod = if (isSucculentOrCactus) "Check that the soil is 100% dry before watering." else "Insert finger 2 inches into soil; water when dry.",
            targetDrynessPercentage = if (isSucculentOrCactus) "90% – 100% Dryout" else "50% – 60% Substrate Dryout",
            wateringTechnique = "Drench thoroughly until water exits drainage holes; discard excess tray runoff.",
            waterQualityRecommendation = "Room-temperature filtered or settled tap water.",
            summerVsWinterAdjust = "Increase watering during spring/summer active growth; reduce by 30-50% in winter dormancy.",
            overwateringSigns = "Yellow lower leaves, blackened mushy stems, heavy wet pot.",
            underwateringSigns = "Curling leaves, dry crispy margins, drooping foliage."
        )

        val proTips = BotanicalProTip(
            humidityRange = humidDesc,
            temperatureRange = "18°C – 28°C (65°F – 82°F) • Protect from drafts",
            petSafetyStatus = if (isAroid) "Mildly Toxic (Calcium Oxalates)" else "Check Pet Safety Precaution",
            isPetSafe = !isAroid,
            toxicityDetails = if (isAroid) "Contains insoluble oxalates; keep out of reach of domestic pets." else "Keep away from pets as a general precaution.",
            leafMaintenance = "Gently wipe foliage with a damp cloth monthly to maximize photosynthesis.",
            seasonalGrowthHabit = "Active vegetative growth in spring and summer; resting in winter.",
            botanicalTrivia = "Belongs to the $fam family, adapted to native climatic conditions."
        )

        return SpeciesCareGuide(
            scientificName = sci,
            commonName = com,
            family = fam,
            sunlight = sunlight,
            soil = soil,
            watering = watering,
            proTips = proTips
        )
    }

    /**
     * Returns curated botanical encyclopedia entries for the Plant Library screen.
     */
    fun getCuratedLibrary(): List<SpeciesCareGuide> {
        val emptyCare = com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan()
        val popularSpecies = listOf(
            Triple("Monstera deliciosa", "Swiss Cheese Plant", "Araceae"),
            Triple("Ficus lyrata", "Fiddle-Leaf Fig", "Moraceae"),
            Triple("Sansevieria trifasciata", "Snake Plant", "Asparagaceae"),
            Triple("Epipremnum aureum", "Golden Pothos", "Araceae"),
            Triple("Spathiphyllum wallisii", "Peace Lily", "Araceae"),
            Triple("Zamioculcas zamiifolia", "ZZ Plant", "Araceae"),
            Triple("Chlorophytum comosum", "Spider Plant", "Asparagaceae"),
            Triple("Calathea orbifolia", "Prayer Plant", "Marantaceae"),
            Triple("Aloe barbadensis miller", "Aloe Vera", "Asphodelaceae"),
            Triple("Ficus elastica", "Rubber Tree", "Moraceae"),
            Triple("Crassula ovata", "Jade Plant", "Crassulaceae"),
            Triple("Pilea peperomioides", "Chinese Money Plant", "Urticaceae")
        )

        return popularSpecies.map { (sci, com, fam) ->
            findExactOrFamilyMatch(sci, com, fam, emptyCare)
        }
    }
}
