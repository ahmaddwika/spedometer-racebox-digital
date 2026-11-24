package com.speedometer.app

import android.app.Application
import com.speedometer.app.data.repository.TripRepository

class SpeedometerApp : Application() {

    lateinit var tripRepository: TripRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tripRepository = TripRepository(this)
    }
}
