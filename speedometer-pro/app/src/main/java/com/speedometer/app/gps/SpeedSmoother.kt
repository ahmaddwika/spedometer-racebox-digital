package com.speedometer.app.gps

import kotlin.math.abs

/**
 * Responsive adaptive smoother for the displayed speed.
 *
 * GPS Doppler speed is already accurate and instantaneous, so this should only
 * prevent digit flicker, NOT add lag. The alpha adapts to how fast the value
 * changes: big jumps snap almost instantly, small wiggles get light smoothing.
 */
class SpeedSmoother(
    private val slowAlpha: Float = 0.4f,
    private val midAlpha: Float = 0.7f,
    private val fastAlpha: Float = 0.9f
) {

    private var currentKmh: Float = 0f
    private var initialized = false

    fun push(rawKmh: Float): Float {
        if (!initialized || rawKmh <= 0f) {
            currentKmh = rawKmh
            initialized = true
            return currentKmh
        }
        val diff = abs(rawKmh - currentKmh)
        val alpha = when {
            diff > 10f -> fastAlpha
            diff > 3f -> midAlpha
            else -> slowAlpha
        }
        currentKmh = currentKmh + (rawKmh - currentKmh) * alpha
        return currentKmh
    }

    fun reset() {
        currentKmh = 0f
        initialized = false
    }
}
