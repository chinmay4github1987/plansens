package com.prasjaychi.plantsense.ui.trends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity
import com.prasjaychi.plantsense.ui.theme.Amber500
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Rose500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Interactive Compose Canvas line & area chart visualizing longitudinal plant health score trends.
 * Supports smooth cubic bezier curves, gradient fill, horizontal threshold zones, and interactive touch scrubbing.
 */
@Composable
fun PlantHealthScoreCanvasChart(
    records: List<PlantHealthRecordEntity>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No health checkup points recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Selected data point for scrubber
    var selectedRecordIndex by remember { mutableStateOf<Int?>(null) }
    val activeRecord = selectedRecordIndex?.let { records.getOrNull(it) }

    val primaryColor = Emerald500
    val secondaryColor = Cyan400
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val optimalZoneColor = Emerald500.copy(alpha = 0.06f)
    val criticalZoneColor = Rose500.copy(alpha = 0.05f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Scrubber Tooltip Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = activeRecord != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (activeRecord != null) {
                    ScrubberTooltipHeader(record = activeRecord)
                }
            }

            if (activeRecord == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Text(
                        text = "Touch or drag across chart to inspect recorded health scores",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .testTag("health_trend_canvas_box")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(records) {
                        detectTapGestures(
                            onPress = { offset ->
                                selectedRecordIndex = findNearestIndex(offset.x, size.width.toFloat(), records.size)
                            },
                            onTap = { offset ->
                                selectedRecordIndex = findNearestIndex(offset.x, size.width.toFloat(), records.size)
                            }
                        )
                    }
                    .pointerInput(records) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                selectedRecordIndex = findNearestIndex(change.position.x, size.width.toFloat(), records.size)
                            },
                            onDragEnd = {
                                // keep active selection for inspection
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 40f
                val paddingRight = 24f
                val paddingTop = 20f
                val paddingBottom = 36f

                val plotWidth = width - paddingLeft - paddingRight
                val plotHeight = height - paddingTop - paddingBottom

                // 1. Draw Threshold Background Zones
                // Optimal Zone: >= 75% health score (top 25% of plot)
                val y75 = paddingTop + plotHeight * (1f - 0.75f)
                drawRect(
                    color = optimalZoneColor,
                    topLeft = Offset(paddingLeft, paddingTop),
                    size = Size(plotWidth, y75 - paddingTop)
                )

                // Critical Warning Zone: <= 50% health score (bottom 50% of plot)
                val y50 = paddingTop + plotHeight * (1f - 0.50f)
                drawRect(
                    color = criticalZoneColor,
                    topLeft = Offset(paddingLeft, y50),
                    size = Size(plotWidth, paddingTop + plotHeight - y50)
                )

                // 2. Draw Horizontal Grid Lines & Score Axis Markers (0, 25, 50, 75, 100)
                val gridScores = listOf(0, 25, 50, 75, 100)
                gridScores.forEach { score ->
                    val y = paddingTop + plotHeight * (1f - (score / 100f))
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f,
                        pathEffect = if (score == 75 || score == 50) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                    )
                }

                // 3. Compute Coordinates for each Record
                val points = records.mapIndexed { index, record ->
                    val x = if (records.size > 1) {
                        paddingLeft + (index.toFloat() / (records.size - 1)) * plotWidth
                    } else {
                        paddingLeft + plotWidth / 2f
                    }
                    val normalizedScore = (record.healthScore.toFloat() / 100f).coerceIn(0f, 1f)
                    val y = paddingTop + plotHeight * (1f - normalizedScore)
                    Offset(x, y)
                }

                // 4. Draw Gradient Fill Area underneath Curve
                if (points.isNotEmpty()) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, paddingTop + plotHeight)
                        lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }

                        lineTo(points.last().x, paddingTop + plotHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                secondaryColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = paddingTop,
                            endY = paddingTop + plotHeight
                        )
                    )

                    // 5. Draw Spline Line
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )

                    // 6. Draw Node Circles
                    points.forEachIndexed { idx, point ->
                        val isSelected = idx == selectedRecordIndex
                        val nodeColor = when {
                            records[idx].healthScore >= 80 -> Emerald500
                            records[idx].healthScore >= 60 -> Amber500
                            else -> Rose500
                        }

                        // Outer halo
                        drawCircle(
                            color = if (isSelected) primaryColor.copy(alpha = 0.35f) else nodeColor.copy(alpha = 0.15f),
                            radius = if (isSelected) 14f else 8f,
                            center = point
                        )

                        // Solid node
                        drawCircle(
                            color = if (isSelected) Color.White else nodeColor,
                            radius = if (isSelected) 7f else 4.5f,
                            center = point
                        )

                        if (isSelected) {
                            drawCircle(
                                color = primaryColor,
                                radius = 4f,
                                center = point
                            )
                        }
                    }

                    // 7. Scrubber Active Vertical Guide Indicator
                    selectedRecordIndex?.let { selIdx ->
                        if (selIdx in points.indices) {
                            val selPoint = points[selIdx]
                            drawLine(
                                color = primaryColor.copy(alpha = 0.7f),
                                start = Offset(selPoint.x, paddingTop),
                                end = Offset(selPoint.x, paddingTop + plotHeight),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        }
                    }
                }
            }
        }

        // Horizontal X-Axis Time Bounds Labels
        if (records.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 24.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDateShort(records.first().timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (records.size > 2) {
                    Text(
                        text = formatDateShort(records[records.size / 2].timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDateShort(records.last().timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Interactive Canvas chart displaying soil moisture levels and hydration compliance.
 */
@Composable
fun PlantMoistureCanvasChart(
    records: List<PlantHealthRecordEntity>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) return

    val cyanColor = Cyan400
    val amberColor = Amber500
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Soil Moisture Tracking (% Saturation)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(cyanColor)
                )
                Text(
                    text = "Optimal Zone (40-70%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padL = 36f
                val padR = 20f
                val padT = 16f
                val padB = 24f

                val plotW = width - padL - padR
                val plotH = height - padT - padB

                // Draw Optimal Hydration Zone (40% to 70%)
                val y70 = padT + plotH * (1f - 0.70f)
                val y40 = padT + plotH * (1f - 0.40f)
                drawRect(
                    color = cyanColor.copy(alpha = 0.08f),
                    topLeft = Offset(padL, y70),
                    size = Size(plotW, y40 - y70)
                )

                // Reference lines at 40% and 70%
                drawLine(
                    color = cyanColor.copy(alpha = 0.3f),
                    start = Offset(padL, y70),
                    end = Offset(width - padR, y70),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                drawLine(
                    color = cyanColor.copy(alpha = 0.3f),
                    start = Offset(padL, y40),
                    end = Offset(width - padR, y40),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Draw Moisture Bars
                val barWidth = (plotW / records.size).coerceAtMost(28f)
                records.forEachIndexed { i, record ->
                    val cx = if (records.size > 1) {
                        padL + (i.toFloat() / (records.size - 1)) * (plotW - barWidth) + barWidth / 2f
                    } else {
                        padL + plotW / 2f
                    }
                    val barH = plotH * record.soilMoistureLevel.coerceIn(0.05f, 1f)
                    val barTop = padT + plotH - barH

                    val barColor = when {
                        record.soilMoistureLevel in 0.4f..0.7f -> cyanColor
                        record.soilMoistureLevel > 0.7f -> Emerald500
                        else -> amberColor
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(cx - barWidth / 2f, barTop),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}

/**
 * Segmented distribution bar showing breakdown of health vitality categories.
 */
@Composable
fun VitalityDistributionCanvasBar(
    breakdown: Map<String, Int>,
    total: Int,
    modifier: Modifier = Modifier
) {
    if (total == 0) return

    val thrivingCount = breakdown["THRIVING"] ?: 0
    val healthyCount = breakdown["HEALTHY"] ?: 0
    val attentionCount = breakdown["NEEDS_ATTENTION"] ?: 0
    val criticalCount = breakdown["CRITICAL"] ?: 0

    val thrivingRatio = thrivingCount.toFloat() / total
    val healthyRatio = healthyCount.toFloat() / total
    val attentionRatio = attentionCount.toFloat() / total
    val criticalRatio = criticalCount.toFloat() / total

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Vitality Category Distribution",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startX = 0f
                val w = size.width
                val h = size.height

                // Thriving
                if (thrivingRatio > 0) {
                    val segmentW = w * thrivingRatio
                    drawRect(Emerald500, Offset(startX, 0f), Size(segmentW, h))
                    startX += segmentW
                }
                // Healthy
                if (healthyRatio > 0) {
                    val segmentW = w * healthyRatio
                    drawRect(Cyan400, Offset(startX, 0f), Size(segmentW, h))
                    startX += segmentW
                }
                // Needs Attention
                if (attentionRatio > 0) {
                    val segmentW = w * attentionRatio
                    drawRect(Amber500, Offset(startX, 0f), Size(segmentW, h))
                    startX += segmentW
                }
                // Critical
                if (criticalRatio > 0) {
                    val segmentW = w * criticalRatio
                    drawRect(Rose500, Offset(startX, 0f), Size(segmentW, h))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VitalityLegendItem("Thriving", thrivingCount, Emerald500)
            VitalityLegendItem("Healthy", healthyCount, Cyan400)
            VitalityLegendItem("Attention", attentionCount, Amber500)
            VitalityLegendItem("Critical", criticalCount, Rose500)
        }
    }
}

@Composable
private fun VitalityLegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScrubberTooltipHeader(record: PlantHealthRecordEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = record.plantName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        record.healthScore >= 80 -> Emerald500.copy(alpha = 0.2f)
                        record.healthScore >= 60 -> Amber500.copy(alpha = 0.2f)
                        else -> Rose500.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = "${record.healthScore}%",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            record.healthScore >= 80 -> Emerald500
                            record.healthScore >= 60 -> Amber500
                            else -> Rose500
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = record.vitalityStatus,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun findNearestIndex(touchX: Float, totalWidth: Float, recordCount: Int): Int {
    if (recordCount <= 1) return 0
    val paddingLeft = 40f
    val paddingRight = 24f
    val plotWidth = totalWidth - paddingLeft - paddingRight
    val clampedX = (touchX - paddingLeft).coerceIn(0f, plotWidth)
    val ratio = clampedX / plotWidth
    return (ratio * (recordCount - 1)).toInt().coerceIn(0, recordCount - 1)
}

private fun formatDateShort(timestampMs: Long): String {
    return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestampMs))
}
