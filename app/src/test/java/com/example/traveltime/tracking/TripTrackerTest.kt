package com.example.traveltime.tracking

import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.TravelMode.BICYCLE
import com.example.traveltime.data.TravelMode.VEHICLE
import com.example.traveltime.data.TravelMode.WALKING
import com.example.traveltime.data.Trip
import com.example.traveltime.data.TripStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

class FakeStore : TripStore {
    val trips = mutableListOf<Trip>()
    private var nextId = 1L
    override suspend fun ongoing() = trips.filter { it.endMillis == null }.maxByOrNull { it.startMillis }
    override suspend fun lastFinished() = trips.filter { it.endMillis != null }.maxByOrNull { it.endMillis!! }
    override suspend fun insert(trip: Trip): Long = nextId++.also { trips += trip.copy(id = it) }
    override suspend fun update(trip: Trip) {
        trips[trips.indexOfFirst { it.id == trip.id }] = trip
    }
    override suspend fun pruneShort(endedBefore: Long, minDurationMillis: Long): Int {
        val before = trips.size
        trips.removeAll { it.auto && it.endMillis != null && it.endMillis < endedBefore && it.endMillis - it.startMillis < minDurationMillis }
        return before - trips.size
    }
}

class TripTrackerTest {
    private val store = FakeStore()
    private val events = mutableListOf<String>()
    private val tracker = TripTracker(
        store = store,
        config = { TrackingConfig(setOf(VEHICLE, BICYCLE), minDurationMillis = 3 * MIN, mergeGapMillis = 5 * MIN) },
        listener = object : TripListener {
            override fun onTripStarted(trip: Trip) { events += "start ${trip.mode}" }
            override fun onTripEnded(trip: Trip) { events += "end ${trip.mode}" }
        },
    )

    private suspend fun enter(mode: TravelMode, atMin: Long) = tracker.onTransition(mode, true, atMin * MIN)
    private suspend fun exit(mode: TravelMode, atMin: Long) = tracker.onTransition(mode, false, atMin * MIN)

    @Test
    fun Should_RecordTrip_When_EnteringAndExitingVehicle() = runTest {
        enter(VEHICLE, 0)
        exit(VEHICLE, 30)
        assertEquals(listOf(Trip(1, VEHICLE, 0, 30 * MIN)), store.trips)
        assertEquals(listOf("start VEHICLE", "end VEHICLE"), events)
    }

    @Test
    fun Should_ResumeTrip_When_StopIsShorterThanMergeGap() = runTest {
        enter(VEHICLE, 0)
        exit(VEHICLE, 10)
        enter(VEHICLE, 13) // red light
        exit(VEHICLE, 40)
        assertEquals(1, store.trips.size)
        assertEquals(40 * MIN, store.trips.single().durationMillis(0))
    }

    @Test
    fun Should_StartNewTrip_When_StopIsLongerThanMergeGap() = runTest {
        enter(VEHICLE, 0)
        exit(VEHICLE, 10)
        enter(VEHICLE, 30)
        exit(VEHICLE, 40)
        assertEquals(2, store.trips.size)
    }

    @Test
    fun Should_IgnoreMode_When_NotEnabled() = runTest {
        enter(WALKING, 0)
        exit(WALKING, 20)
        assertTrue(store.trips.isEmpty())
    }

    @Test
    fun Should_SwitchTrips_When_DifferentModeEntered() = runTest {
        enter(VEHICLE, 0)
        enter(BICYCLE, 20)
        exit(BICYCLE, 35)
        assertEquals(listOf(VEHICLE, BICYCLE), store.trips.map { it.mode })
        assertEquals(20 * MIN, store.trips[0].endMillis)
    }

    @Test
    fun Should_PruneShortTrip_When_ItCanNoLongerBeResumed() = runTest {
        enter(VEHICLE, 0)
        exit(VEHICLE, 1) // 1-minute false positive
        assertEquals(1, store.trips.size) // kept while it could still be resumed
        enter(BICYCLE, 30)
        assertEquals(listOf(BICYCLE), store.trips.map { it.mode })
    }

    @Test
    fun Should_NotTouchManualTrip_When_DetectionFires() = runTest {
        tracker.startManual(WALKING, 0)
        enter(VEHICLE, 5)
        exit(WALKING, 10)
        val trip = store.trips.single()
        assertEquals(WALKING, trip.mode)
        assertNull(trip.endMillis)
        tracker.stopOngoing(20 * MIN)
        assertEquals(20 * MIN, store.trips.single().endMillis)
    }

    @Test
    fun Should_IgnoreExit_When_NoMatchingTripRuns() = runTest {
        exit(VEHICLE, 5)
        assertTrue(store.trips.isEmpty())
    }
}
