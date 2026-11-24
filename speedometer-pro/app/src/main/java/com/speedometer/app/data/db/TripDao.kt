package com.speedometer.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: Trip): Long

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun observeAll(): LiveData<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    suspend fun getAll(): List<Trip>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
