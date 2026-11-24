package com.speedometer.app.gps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth shared between [GpsService] and the UI.
 * The service pushes raw GPS fixes through TripTracker and publishes
 * the resulting state here; screens just observe the StateFlow.
 */
object GpsStateHolder {

    data class UiState(
        val running: Boolean = false,
        val gpsFix: Boolean = false,
        val speedKmh: Float = 0f,
        val smoothedKmh: Float = 0f,
        val maxSpeedKmh: Float = 0f,
        val distanceM: Double = 0.0,
        val elapsedMs: Long = 0L,
        val accuracyM: Float = 0f,
        val satellites: Int = 0,
        val message: String = ""
    )

    val tracker = TripTracker()
    val smoother = SpeedSmoother()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun publish(partial: UiState) {
        _state.value = partial
    }

    fun current(): UiState = _state.value

    fun resetSession() {
        tracker.reset()
        smoother.reset()
    }
}
