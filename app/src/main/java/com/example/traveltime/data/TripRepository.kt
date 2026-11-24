package com.example.traveltime.data

import kotlinx.coroutines.flow.Flow

/** The persistence operations the trip-detection logic needs. Kept small so it can be faked in tests. */
interface TripStore {
    suspend fun ongoing(): Trip?
    suspend fun lastFinished(): Trip?
    suspend fun insert(trip: Trip): Long
    suspend fun update(trip: Trip)
    suspend fun pruneShort(endedBefore: Long, minDurationMillis: Long): Int
}

class TripRepository(private val dao: TripDao) : TripStore {
    fun observeAll(): Flow<List<Trip>> = dao.observeAll()
    suspend fun getAll(): List<Trip> = dao.getAll()
    suspend fun get(id: Long): Trip? = dao.get(id)
    suspend fun delete(trip: Trip) = dao.delete(trip)
    suspend fun deleteAll() = dao.deleteAll()

    override suspend fun ongoing(): Trip? = dao.ongoing()
    override suspend fun lastFinished(): Trip? = dao.lastFinished()
    override suspend fun insert(trip: Trip): Long = dao.insert(trip)
    override suspend fun update(trip: Trip) = dao.update(trip)
    override suspend fun pruneShort(endedBefore: Long, minDurationMillis: Long): Int =
        dao.pruneShort(endedBefore, minDurationMillis)
}
