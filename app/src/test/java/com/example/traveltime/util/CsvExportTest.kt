package com.example.traveltime.util

import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.Trip
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CsvExportTest {
    private val zone = ZoneOffset.UTC
    private fun at(d: Int, h: Int, m: Int) = LocalDateTime.of(2026, 11, d, h, m).toInstant(zone).toEpochMilli()

    @Test
    fun Should_WriteOneRowPerTripInChronologicalOrder() {
        val trips = listOf(
            Trip(2, TravelMode.BICYCLE, at(24, 17, 0), at(24, 17, 25), auto = false, note = "home, \"fast\""),
            Trip(1, TravelMode.VEHICLE, at(24, 8, 10), at(24, 9, 15)),
        )
        val lines = CsvExport.build(trips, zone, nowMillis = 0).split("\r\n")
        assertEquals("date,start,end,duration_minutes,duration,mode,source,note", lines[0])
        assertEquals("2026-11-24,08:10,09:15,65,1:05,Vehicle,auto,", lines[1])
        assertEquals("2026-11-24,17:00,17:25,25,0:25,Bicycle,manual,\"home, \"\"fast\"\"\"", lines[2])
    }

    @Test
    fun Should_IncludeEndDate_When_TripCrossesMidnight() {
        val trips = listOf(Trip(1, TravelMode.VEHICLE, at(24, 23, 30), at(25, 0, 45)))
        val row = CsvExport.build(trips, zone, 0).split("\r\n")[1]
        assertEquals("2026-11-24,23:30,2026-11-25 00:45,75,1:15,Vehicle,auto,", row)
    }

    @Test
    fun Should_MarkOngoingTrip() {
        val trips = listOf(Trip(1, TravelMode.VEHICLE, at(24, 8, 0), null))
        val row = CsvExport.build(trips, zone, nowMillis = at(24, 8, 20)).split("\r\n")[1]
        assertEquals("2026-11-24,08:00,ongoing,20,0:20,Vehicle,auto,", row)
    }
}
