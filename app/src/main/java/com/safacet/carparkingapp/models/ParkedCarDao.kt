package com.safacet.carparkingapp.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ParkedCarDao {
    @Query("SELECT * FROM parked_car ORDER BY time DESC")
    fun getAll(): MutableList<ParkedCar>

    @Query("SELECT * FROM parked_car WHERE time LIKE :time LIMIT 1")
    fun findByTime(time: Long): ParkedCar

    @Insert
    fun insertAll(vararg parkedCar: ParkedCar)

    @Delete
    fun delete(parkedCar: ParkedCar)
}