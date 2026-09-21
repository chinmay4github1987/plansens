package com.prasjaychi.plantsense.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.care.PlantCareTipsProvider
import com.prasjaychi.plantsense.data.care.SoilCareAdvice
import com.prasjaychi.plantsense.data.care.SpeciesCareGuide
import com.prasjaychi.plantsense.data.care.SubstrateComponent
import com.prasjaychi.plantsense.data.care.SunlightCareAdvice
import com.prasjaychi.plantsense.data.care.WateringCareAdvice
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Indigo800
import com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport

/**
 * Filter categories for the Plant Care Tips section.
 */
enum class CareTipFilter(val label: String, val icon: ImageVector) {
    ALL("All Advice", Icons.Default.Spa),
    SUNLIGHT("Sunlight", Icons.Default.WbSunny),
    SOIL("Soil & Substrate", Icons.Default.Eco),
    WATERING("Watering", Icons.Default.WaterDrop),
    PRO_TIPS("Pro Tips & Safety", Icons.Default.Pets)
}

/**
 * Material 3 Plant Care Tips section delivering contextual care advice
 * for Sunlight, Soil, and Watering tailored directly to the species identified by Gemini AI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantCareTipsSection(
    report: PlantDiagnosisReport,
    modifier: Modifier = Modifier
) {
    val careGuide = remember(report) {
        PlantCareTipsProvider.getCareGuideFor(report)
    }

    var selectedFilter by remember { mutableStateOf(CareTipFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plant_care_tips_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Species Context Header Card
        SpeciesCareHeaderCard(careGuide = careGuide)

        // Filter chips row (All, Sunlight, Soil, Watering, Pro Tips)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(CareTipFilter.values().size) { index ->
                val filter = CareTipFilter.values()[index]
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald500.copy(alpha = 0.15f),
                        selectedLabelColor = Emerald600,
                        selectedLeadingIconColor = Emerald600
                    ),
                    modifier = Modifier.testTag("care_tip_filter_${filter.name.lowercase()}")
                )
            }
        }

        // Section 1: Sunlight & Lighting Advice
        if (selectedFilter == CareTipFilter.ALL || selectedFilter == CareTipFilter.SUNLIGHT) {
            SunlightAdviceCard(sunlight = careGuide.sunlight)
        }

        // Section 2: Soil & Substrate Advice
        if (selectedFilter == CareTipFilter.ALL || selectedFilter == CareTipFilter.SOIL) {
            SoilAdviceCard(soil = careGuide.soil)
        }

        // Section 3: Watering & Moisture Advice
        if (selectedFilter == CareTipFilter.ALL || selectedFilter == CareTipFilter.WATERING) {
            WateringAdviceCard(watering = careGuide.watering)
        }

        // Section 4: Botanical Pro Tips, Pet Safety & Atmosphere
        if (selectedFilter == CareTipFilter.ALL || selectedFilter == CareTipFilter.PRO_TIPS) {
            BotanicalProTipsCard(
                proTips = careGuide.proTips,
                scientificName = careGuide.scientificName
            )
        }
    }
}

/**
 * Top contextual banner summarizing species identity and instant care badges.
 */
@Composable
private fun SpeciesCareHeaderCard(careGuide: SpeciesCareGuide) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Emerald500.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SPECIES CARE PROTOCOL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = careGuide.family,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = careGuide.commonName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = careGuide.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Quick Spec Summary Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickCareBadge(
                    icon = Icons.Default.WbSunny,
                    label = "Sunlight",
                    value = careGuide.sunlight.category.split(" ").take(2).joinToString(" "),
                    tint = Color(0xFFD97706)
                )
                QuickCareBadge(
                    icon = Icons.Default.WaterDrop,
                    label = "Dryout",
                    value = careGuide.watering.targetDrynessPercentage.split(" ").first(),
                    tint = Cyan500
                )
                QuickCareBadge(
                    icon = Icons.Default.Eco,
                    label = "Soil pH",
                    value = careGuide.soil.optimalPh.split(" ").take(3).joinToString(" "),
                    tint = Emerald500
                )
                QuickCareBadge(
                    icon = Icons.Default.Pets,
                    label = "Pet Safe",
                    value = if (careGuide.proTips.isPetSafe) "Non-Toxic" else "Caution",
                    tint = if (careGuide.proTips.isPetSafe) Emerald500 else Color(0xFFE11D48)
                )
            }
        }
    }
}

@Composable
private fun QuickCareBadge(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Sunlight & Lighting Advice Card with Lux Gauge, Placement Compass, and Light Stress warnings.
 */
@Composable
private fun SunlightAdviceCard(sunlight: SunlightCareAdvice) {
    val amberColor = Color(0xFFD97706)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("care_tips_sunlight_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, amberColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = amberColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = amberColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sunlight & Photoperiod Advice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sunlight.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = amberColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Photoperiod and Lux Intensity Gauge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target Lux Intensity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sunlight.luxRange,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = amberColor
                        )
                        Text(
                            text = "(${sunlight.footCandles})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = amberColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = amberColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sunlight.dailyExposureHours,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = amberColor
                            )
                        }
                    }
                }
            }

            // Ideal Placement & Orientation
            CareTipDetailRow(
                icon = Icons.Default.CompassCalibration,
                iconTint = amberColor,
                title = "Recommended Window Placement",
                content = "${sunlight.idealPlacement} (Best: ${sunlight.windowOrientation})"
            )

            // Tolerance & Spectrum
            CareTipDetailRow(
                icon = Icons.Default.LightMode,
                iconTint = amberColor,
                title = "Light Tolerance & Adaptation",
                content = sunlight.lightTolerance
            )

            // Warning: Light Stress Signs
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = amberColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, amberColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = amberColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Light Stress Diagnostics",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = amberColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sunlight.lightStressSigns,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Soil & Substrate Advice Card featuring substrate formulation ratio breakdown bar, pH rating, and repotting cues.
 */
@Composable
private fun SoilAdviceCard(soil: SoilCareAdvice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("care_tips_soil_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald500.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Soil & Substrate Formulation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = soil.mixName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Visual Substrate Formulation Recipe Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Recommended Substrate Blend Ratios:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // Segmented visual bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    soil.components.forEach { comp ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(comp.colorHex))
                        } catch (_: Exception) {
                            Emerald500
                        }
                        Box(
                            modifier = Modifier
                                .weight(comp.percentage.toFloat().coerceAtLeast(1f))
                                .height(14.dp)
                                .background(color)
                        )
                    }
                }

                // Legend items
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    soil.components.forEach { comp ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(comp.colorHex))
                        } catch (_: Exception) {
                            Emerald500
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${comp.percentage}% ${comp.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${comp.purpose}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // pH & Drainage badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Substrate pH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(soil.optimalPh, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald600)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Drainage Porosity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(soil.drainageLevel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald600)
                    }
                }
            }

            // Repotting Advice
            CareTipDetailRow(
                icon = Icons.Default.LocalFlorist,
                iconTint = Emerald500,
                title = "Repotting & Pot Preference",
                content = "${soil.repottingFrequency} ${soil.potRecommendation}"
            )
        }
    }
}

/**
 * Watering & Moisture Advice Card featuring moisture check protocol and seasonal rhythm.
 */
@Composable
private fun WateringAdviceCard(watering: WateringCareAdvice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("care_tips_watering_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Cyan500.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Cyan500.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Cyan500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Watering Cadence & Hydration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = watering.cadenceSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Cyan500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Moisture check method step guide
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Cyan500.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Cyan500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Soil Moisture Testing Protocol:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Cyan500
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = watering.moistureCheckMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Technique & Water Quality
            CareTipDetailRow(
                icon = Icons.Default.Opacity,
                iconTint = Cyan500,
                title = "Application Technique",
                content = watering.wateringTechnique
            )

            CareTipDetailRow(
                icon = Icons.Default.FilterDrama,
                iconTint = Cyan500,
                title = "Water Quality & Sensitivity",
                content = watering.waterQualityRecommendation
            )

            CareTipDetailRow(
                icon = Icons.Default.DeviceThermostat,
                iconTint = Cyan500,
                title = "Seasonal Rhythm (Summer vs Winter)",
                content = watering.summerVsWinterAdjust
            )

            // Overwatering vs Underwatering breakdown
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "⚠️ Overwatered: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                        Text(
                            text = watering.overwateringSigns,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "🍂 Thirsty/Dry: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = watering.underwateringSigns,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botanical Pro Tips, Toxicity & Environment Card.
 */
@Composable
private fun BotanicalProTipsCard(
    proTips: com.prasjaychi.plantsense.data.care.BotanicalProTip,
    scientificName: String
) {
    val indigoColor = Indigo600

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("care_tips_pro_tips_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, indigoColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = indigoColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = indigoColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Botanical Pro Tips & Safety",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Habitat insights for $scientificName",
                        style = MaterialTheme.typography.bodySmall,
                        color = indigoColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Pet Safety Status Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (proTips.isPetSafe) Emerald500.copy(alpha = 0.1f) else Color(0xFFE11D48).copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (proTips.isPetSafe) Emerald500.copy(alpha = 0.3f) else Color(0xFFE11D48).copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = if (proTips.isPetSafe) Emerald500 else Color(0xFFE11D48),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = proTips.petSafetyStatus,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (proTips.isPetSafe) Emerald600 else Color(0xFFE11D48)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = proTips.toxicityDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Environment: Humidity & Temp
            CareTipDetailRow(
                icon = Icons.Default.DeviceThermostat,
                iconTint = indigoColor,
                title = "Atmospheric Temperature & Humidity",
                content = "${proTips.humidityRange} • ${proTips.temperatureRange}"
            )

            // Leaf Hygiene
            CareTipDetailRow(
                icon = Icons.Default.Spa,
                iconTint = indigoColor,
                title = "Foliage Cleaning & Chlorophyll Care",
                content = proTips.leafMaintenance
            )

            // Evolutionary Trivia
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = indigoColor.copy(alpha = 0.06f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = indigoColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Botanical Evolutionary Adaptation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = indigoColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = proTips.botanicalTrivia,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CareTipDetailRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
