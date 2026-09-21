package com.prasjaychi.plantsense.ui.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Filter modes for real-time and post-capture leaf contrast enhancement.
 * Accentuates chlorosis, leaf venation, fungal spots, and necrotic lesions
 * before AI diagnostic model inference.
 */
enum class ContrastFilterMode(
    val label: String,
    val shortLabel: String,
    val contrastMultiplier: Float,
    val description: String
) {
    OFF(
        label = "Raw Exposure (Off)",
        shortLabel = "Raw",
        contrastMultiplier = 1.00f,
        description = "Unmodified sensor capture"
    ),
    BALANCED(
        label = "AI Balanced (+25%)",
        shortLabel = "+25% Balanced",
        contrastMultiplier = 1.25f,
        description = "Standard botanical contrast boost for typical indoor leaf conditions"
    ),
    VIVID(
        label = "High Foliage Detail (+45%)",
        shortLabel = "+45% High Detail",
        contrastMultiplier = 1.45f,
        description = "Sharpens interveinal chlorosis and fine vein margins"
    ),
    MAX_DYNAMIC(
        label = "Deep Dynamic Range (+70%)",
        shortLabel = "+70% Dynamic",
        contrastMultiplier = 1.70f,
        description = "Deep contrast stretching for low-light or backlit specimens"
    )
}

/**
 * Live frame telemetry computed in real time by CameraX ImageAnalysis.
 */
data class LiveContrastMetrics(
    val meanLuminance: Float = 128f,
    val contrastScore: Float = 45f,
    val qualityLabel: String = "Optimal Contrast",
    val suggestedBoostPercent: Int = 25,
    val timestamp: Long = System.currentTimeMillis()
)

object PlantContrastFilter {

    /**
     * Applies a hardware-accelerated ColorMatrix contrast enhancement filter to a Bitmap.
     * Accentuates chlorophyll variation and leaf venation prior to AI inference.
     */
    fun applyContrastFilter(
        source: Bitmap,
        mode: ContrastFilterMode = ContrastFilterMode.BALANCED
    ): Bitmap {
        if (mode == ContrastFilterMode.OFF) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val width = source.width
        val height = source.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val scale = mode.contrastMultiplier
        val translate = (-0.5f * scale + 0.5f) * 255f

        // Subtle green channel enhancement (+3%) to optimize botanical leaf structure
        val gScale = scale * 1.03f
        val gTranslate = (-0.5f * gScale + 0.5f) * 255f

        val matrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, gScale, 0f, 0f, gTranslate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return outputBitmap
    }

    /**
     * CameraX ImageAnalysis.Analyzer that computes real-time scene contrast and luminance.
     * Dynamically recommends optimal filter adjustments based on incoming camera frames.
     */
    class LiveContrastAnalyzer(
        private val onMetricsUpdated: (LiveContrastMetrics) -> Unit
    ) : ImageAnalysis.Analyzer {

        private var lastAnalyzedTimestamp = 0L

        override fun analyze(image: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            // Throttle calculations to ~120ms to keep UI responsive and save power
            if (currentTimestamp - lastAnalyzedTimestamp < 120L) {
                image.close()
                return
            }

            try {
                // The first plane in CameraX image is luminance (Y channel)
                val planes = image.planes
                if (planes.isNotEmpty()) {
                    val buffer: ByteBuffer = planes[0].buffer
                    val remaining = buffer.remaining()

                    if (remaining > 0) {
                        // Sample pixels evenly across the frame
                        val sampleStep = max(1, remaining / 1200)
                        var sampleCount = 0
                        var sum = 0.0
                        var sumSq = 0.0

                        var i = 0
                        while (i < remaining) {
                            val pixel = buffer.get(i).toInt() and 0xFF
                            sum += pixel
                            sumSq += (pixel * pixel)
                            sampleCount++
                            i += sampleStep
                        }

                        if (sampleCount > 0) {
                            val mean = (sum / sampleCount).toFloat()
                            val variance = max(0.0, (sumSq / sampleCount) - (mean * mean))
                            val stdDev = sqrt(variance).toFloat()
                            // Normalize contrast index on 0-100 scale
                            val contrastScore = min(100f, (stdDev / 128f) * 100f)

                            val (qualityLabel, boost) = when {
                                contrastScore < 25f -> "Low Contrast (Filter Boost Active)" to 45
                                contrastScore < 45f -> "Moderate Contrast (Enhancing)" to 25
                                contrastScore < 70f -> "Optimal Foliage Lighting" to 25
                                else -> "High Contrast Scene" to 15
                            }

                            val metrics = LiveContrastMetrics(
                                meanLuminance = mean,
                                contrastScore = contrastScore,
                                qualityLabel = qualityLabel,
                                suggestedBoostPercent = boost,
                                timestamp = currentTimestamp
                            )
                            onMetricsUpdated(metrics)
                            lastAnalyzedTimestamp = currentTimestamp
                        }
                    }
                }
            } catch (e: Exception) {
                // Non-critical analysis exception, safely ignore frame
            } finally {
                image.close()
            }
        }
    }
}
