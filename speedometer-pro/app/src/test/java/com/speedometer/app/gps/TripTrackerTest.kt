package com.speedometer.app.gps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTrackerTest {

    private val lat0 = -6.2
    private val lon0 = 106.8

    /** Approx degrees per meter at Jakarta's latitude. */
    private fun metersNorth(m: Double) = m / 111_320.0
    private fun metersEast(m: Double) = m / (111_320.0 * kotlin.math.cos(Math.toRadians(lat0)))

    private fun sample(
        timeMs: Long,
        latOffset: Double = 0.0,
        lonOffset: Double = 0.0,
        speedMps: Float = 0f,
        accuracyM: Float = 4f
    ) = TripTracker.Sample(timeMs, lat0 + latOffset, lon0 + lonOffset, speedMps, accuracyM)

    @Test
    fun standingStill_withJitter_reportsZeroSpeedAndDistance() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        var last = tracker.onSample(sample(1000, latOffset = metersNorth(0.4)))
        tracker.onSample(sample(2000, latOffset = metersNorth(0.6)))
        last = tracker.onSample(sample(3000, latOffset = metersNorth(0.2)))
        tracker.onSample(sample(4000, latOffset = metersNorth(0.5)))

        assertEquals(0.0, last.totalDistanceM, 0.0)
        assertEquals(0f, last.speedKmh)
    }

    @Test
    fun moving_usesDopplerSpeed() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        val result = tracker.onSample(sample(1000, latOffset = metersNorth(16.7), speedMps = 16.7f))

        assertTrue(result.valid)
        assertEquals(60.0f, result.speedKmh, 2.0f)
    }

    @Test
    fun moving_accumulatesPlausibleDistance() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        var result = tracker.onSample(sample(1000, latOffset = metersNorth(16.7), speedMps = 16.7f))
        result = tracker.onSample(sample(2000, latOffset = metersNorth(33.4), speedMps = 16.7f))

        assertEquals(33.4, result.totalDistanceM, 3.0)
        assertEquals(16.7, result.distanceSinceLastM, 2.0)
    }

    @Test
    fun badAccuracy_isRejected_andCountsNoDistance() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        val result = tracker.onSample(
            sample(1000, latOffset = metersNorth(50.0), accuracyM = 35f)
        )

        assertFalse(result.valid)
        assertTrue(result.rejectedReason.orEmpty().startsWith("accuracy"))
        assertEquals(0.0, result.totalDistanceM, 0.0)
        assertEquals(1, tracker.rejectedCount)
    }

    @Test
    fun longGap_doesNotJumpDistance() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        val result = tracker.onSample(
            sample(60_000, latOffset = metersNorth(1000.0), speedMps = 5f)
        )

        assertTrue(result.valid)
        assertEquals(0.0, result.distanceSinceLastM, 0.0)
    }

    @Test
    fun teleport_withoutSpeed_isRejected() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        val result = tracker.onSample(
            sample(1000, latOffset = metersNorth(2000.0), speedMps = 0f)
        )

        assertFalse(result.valid)
        assertTrue(result.rejectedReason.orEmpty().contains("teleport"))
    }

    @Test
    fun maxSpeedTracksHighestValue() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        tracker.onSample(sample(1000, latOffset = metersNorth(8.3), speedMps = 8.3f))
        var result = tracker.onSample(sample(2000, latOffset = metersNorth(22.2), speedMps = 22.2f))
        tracker.onSample(sample(3000, latOffset = metersNorth(44.4), speedMps = 44.4f))
        result = tracker.onSample(sample(4000, latOffset = metersNorth(13.9), speedMps = 13.9f))

        assertEquals(160f, result.maxSpeedKmh, 2f)
    }

    @Test
    fun zeroDopplerSpeed_fallsBackToSmoothedPositionSpeed() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        val result = tracker.onSample(
            sample(1000, latOffset = metersNorth(16.7), speedMps = 0f)
        )

        assertTrue(result.valid)
        assertEquals(60.0f, result.speedKmh, 8.0f)
    }

    @Test
    fun resetClearsSession() {
        val tracker = TripTracker()
        tracker.onSample(sample(0))
        tracker.onSample(sample(1000, latOffset = metersNorth(16.7), speedMps = 16.7f))
        tracker.reset()

        val result = tracker.onSample(sample(2000))
        assertEquals(0.0, result.totalDistanceM, 0.0)
        assertEquals(0f, result.maxSpeedKmh)
    }
}
