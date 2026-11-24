package com.example.traveltime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY startMillis ASC")
    suspend fun getAll(): List<Trip>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: Long): Trip?

    @Query("SELECT * FROM trips WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    suspend fun ongoing(): Trip?

    @Query("SELECT * FROM trips WHERE endMillis IS NOT NULL ORDER BY endMillis DESC LIMIT 1")
    suspend fun lastFinished(): Trip?

    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query(
        "DELETE FROM trips WHERE auto = 1 AND endMillis IS NOT NULL " +
            "AND endMillis < :endedBefore AND (endMillis - startMillis) < :minDurationMillis"
    )
    suspend fun pruneShort(endedBefore: Long, minDurationMillis: Long): Int

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
