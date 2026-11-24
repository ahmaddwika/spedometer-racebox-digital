package com.speedometer.app.gps

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * TripTracker is the accuracy engine of the app.
 *
 * Why naive GPS apps are inaccurate:
 *  1. They compute speed as distance/time between position fixes. GPS position
 *     has 2-5m jitter, so this gives nonsense like 20 km/h while standing still
 *     or 0 km/h while driving. The GPS chip instead reports a true Doppler speed
 *     via [android.location.Location.getSpeed]. We always prefer that.
 *  2. They accumulate distance between every fix, so standing-still jitter adds
 *     phantom meters. We only count real movement segments and gate on accuracy,
 *     time gaps and implausible jumps.
 */
class TripTracker(
    private val maxAccuracyM: Float = DEFAULT_MAX_ACCURACY_M,
    private val minSegmentM: Double = DEFAULT_MIN_SEGMENT_M,
    private val maxGapMs: Long = DEFAULT_MAX_GAP_MS,
    private val maxTeleportSpeedMs: Double = DEFAULT_MAX_TELEPORT_MS
) {

    data class Sample(
        val timeMs: Long,
        val lat: Double,
        val lon: Double,
        val speedMps: Float,
        val accuracyM: Float
    )

    data class Result(
        val valid: Boolean,
        val rejectedReason: String?,
        val speedKmh: Float,
        val distanceSinceLastM: Double,
        val totalDistanceM: Double,
        val maxSpeedKmh: Float,
        val elapsedMs: Long,
        val satelliteCount: Int
    )

    companion object {
        const val DEFAULT_MAX_ACCURACY_M = 20f
        const val DEFAULT_MIN_SEGMENT_M = 1.5
        const val DEFAULT_MAX_GAP_MS = 10_000L
        const val DEFAULT_MAX_TELEPORT_MS = 60.0
        private const val FALLBACK_WINDOW = 3
        private const val EARTH_RADIUS_M = 6_371_000.0
    }

    private var last: Sample? = null
    private val fallbackSpeeds = ArrayDeque<Float>()

    var totalDistanceM: Double = 0.0
        private set
    var maxSpeedKmh: Float = 0f
        private set
    var startTimeMs: Long = 0L
        private set
    var sampleCount: Int = 0
        private set
    var rejectedCount: Int = 0
        private set

    fun reset() {
        last = null
        fallbackSpeeds.clear()
        totalDistanceM = 0.0
        maxSpeedKmh = 0f
        startTimeMs = 0L
        sampleCount = 0
        rejectedCount = 0
    }

    fun onSample(s: Sample, satelliteCount: Int = 0): Result {
        val elapsed = if (startTimeMs == 0L) 0L else s.timeMs - startTimeMs
        if (startTimeMs == 0L) startTimeMs = s.timeMs

        if (s.accuracyM > maxAccuracyM) {
            rejectedCount++
            return Result(
                valid = false,
                rejectedReason = "accuracy ${s.accuracyM}m",
                speedKmh = 0f,
                distanceSinceLastM = 0.0,
                totalDistanceM = totalDistanceM,
                maxSpeedKmh = maxSpeedKmh,
                elapsedMs = elapsed,
                satelliteCount = satelliteCount
            )
        }

        val prev = last
        if (prev == null) {
            last = s
            sampleCount++
            return buildResult(s, distance = 0.0, satelliteCount = satelliteCount)
        }

        val dtMs = s.timeMs - prev.timeMs
        val dist = haversineM(prev.lat, prev.lon, s.lat, s.lon)

        // A long gap between fixes means the phone was paused/offline.
        // Jumping the distance across the gap would fake meters, so skip it.
        if (dtMs > maxGapMs) {
            last = s
            sampleCount++
            return buildResult(s, distance = 0.0, satelliteCount = satelliteCount)
        }

        // Teleport guard: if the position moved farther than physically possible
        // for the elapsed time AND the GPS has no speed confirmation, it is a
        // glitchy fix, not real motion.
        if (dtMs > 0) {
            val impliedMs = dist / (dtMs / 1000.0)
            if (impliedMs > maxTeleportSpeedMs && s.speedMps <= 0f) {
                rejectedCount++
                return Result(
                    valid = false,
                    rejectedReason = "teleport ${impliedMs.toInt()}m/s",
                    speedKmh = 0f,
                    distanceSinceLastM = 0.0,
                    totalDistanceM = totalDistanceM,
                    maxSpeedKmh = maxSpeedKmh,
                    elapsedMs = elapsed,
                    satelliteCount = satelliteCount
                )
            }
        }

        // Only count real movement: ignore sub-noise displacement (jitter).
        val accDist = if (dist >= minSegmentM) dist else 0.0
        totalDistanceM += accDist

        // Doppler speed fallback: on some devices the fused provider zeroes
        // speed. Keep a smoothed haversine/dt estimate for those cases, but
        // only for real movement segments, otherwise standing-still jitter
        // would be reported as speed.
        val fallback = if (dtMs > 0 && dist >= minSegmentM) {
            (dist / (dtMs / 1000.0)).toFloat()
        } else 0f
        fallbackSpeeds.addLast(fallback)
        while (fallbackSpeeds.size > FALLBACK_WINDOW) fallbackSpeeds.removeFirst()

        last = s
        sampleCount++
        val result = buildResult(s, distance = accDist, satelliteCount = satelliteCount)
        if (result.speedKmh > maxSpeedKmh) maxSpeedKmh = result.speedKmh
        return result.copy(maxSpeedKmh = maxSpeedKmh)
    }

    fun isRunning(): Boolean = startTimeMs != 0L

    private fun buildResult(
        s: Sample,
        distance: Double,
        satelliteCount: Int
    ): Result {
        val speed = effectiveSpeedMps(s)
        val kmh = speed * 3.6f
        return Result(
            valid = true,
            rejectedReason = null,
            speedKmh = kmh,
            distanceSinceLastM = distance,
            totalDistanceM = totalDistanceM,
            maxSpeedKmh = maxSpeedKmh,
            elapsedMs = if (isRunning()) s.timeMs - startTimeMs else 0L,
            satelliteCount = satelliteCount
        )
    }

    /** Doppler speed preferred; smoothed haversine/dt as fallback. */
    private fun effectiveSpeedMps(s: Sample): Float {
        if (s.speedMps > 0f) return s.speedMps
        if (fallbackSpeeds.isEmpty()) return 0f
        return fallbackSpeeds.average().toFloat()
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * sqrt(a)
    }
}
