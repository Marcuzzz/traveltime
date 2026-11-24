package com.example.traveltime.util

import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.Trip
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TripStatsTest {
    private val min = 60_000L

    @Test
    fun Should_ClipTrips_When_TheyCrossTheRangeEdges() {
        val trips = listOf(
            Trip(1, TravelMode.VEHICLE, startMillis = 0, endMillis = 20 * min),
            Trip(2, TravelMode.BICYCLE, startMillis = 50 * min, endMillis = 70 * min),
        )
        assertEquals(20 * min, totalInRange(trips, 10 * min, 60 * min, nowMillis = 0))
    }

    @Test
    fun Should_CountUntilNow_When_TripIsOngoing() {
        val trips = listOf(Trip(1, TravelMode.VEHICLE, startMillis = 0, endMillis = null))
        assertEquals(7 * min, totalInRange(trips, 0, Long.MAX_VALUE, nowMillis = 7 * min))
    }

    @Test
    fun Should_StartWeekOnMonday() {
        val wednesday = LocalDate.of(2026, 11, 25)
        assertEquals(startOfDay(LocalDate.of(2026, 11, 23), ZoneOffset.UTC), startOfWeek(wednesday, ZoneOffset.UTC))
    }

    @Test
    fun Should_FormatDurations() {
        assertEquals("42 s", formatDuration(42_000))
        assertEquals("12 min", formatDuration(12 * min))
        assertEquals("1 h 05 min", formatDuration(65 * min))
        assertEquals("1:05:09", formatClock(65 * min + 9_000))
    }
}
