package com.speedometer.app.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.speedometer.app.data.db.AppDatabase
import com.speedometer.app.data.db.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TripRepository(context: Context) {

    private val dao = AppDatabase.get(context).tripDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    val trips: LiveData<List<Trip>> = dao.observeAll()

    fun saveTrip(trip: Trip) {
        scope.launch { dao.insert(trip) }
    }

    fun deleteTrip(id: Long) {
        scope.launch { dao.deleteById(id) }
    }

    fun clearAll() {
        scope.launch { dao.deleteAll() }
    }
}
