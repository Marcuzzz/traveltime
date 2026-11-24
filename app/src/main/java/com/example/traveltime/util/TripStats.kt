package com.example.traveltime.util

import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.Trip
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Total travel time inside [fromMillis, toMillis), clipping trips that cross the edges. */
fun totalInRange(trips: List<Trip>, fromMillis: Long, toMillis: Long, nowMillis: Long): Long =
    trips.sumOf { overlap(it, fromMillis, toMillis, nowMillis) }

fun totalsByMode(trips: List<Trip>, fromMillis: Long, toMillis: Long, nowMillis: Long): Map<TravelMode, Long> =
    trips.groupBy { it.mode }
        .mapValues { (_, list) -> totalInRange(list, fromMillis, toMillis, nowMillis) }
        .filterValues { it > 0 }

private fun overlap(trip: Trip, from: Long, to: Long, now: Long): Long {
    val start = maxOf(trip.startMillis, from)
    val end = minOf(trip.endMillis ?: now, to)
    return (end - start).coerceAtLeast(0)
}

fun startOfDay(date: LocalDate, zone: ZoneId): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

fun startOfWeek(today: LocalDate, zone: ZoneId): Long =
    startOfDay(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), zone)

fun startOfMonth(today: LocalDate, zone: ZoneId): Long = startOfDay(today.withDayOfMonth(1), zone)

fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Groups trips (already sorted newest first) by the local day they started on. */
fun groupByDay(trips: List<Trip>, zone: ZoneId): List<Pair<LocalDate, List<Trip>>> =
    trips.groupBy { it.startMillis.toLocalDate(zone) }.toList()
