package com.speedometer.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val distanceM: Double,
    val maxSpeedKmh: Float,
    val avgSpeedKmh: Float
)
