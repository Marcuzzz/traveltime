package com.example.traveltime.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One journey. [endMillis] is null while the trip is still in progress.
 * [auto] is true when the trip was created by activity detection, false when started manually.
 */
@Entity(tableName = "trips", indices = [Index("startMillis"), Index("endMillis")])
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: TravelMode,
    val startMillis: Long,
    val endMillis: Long? = null,
    val auto: Boolean = true,
    val note: String = "",
) {
    val isOngoing: Boolean get() = endMillis == null

    fun durationMillis(now: Long): Long = ((endMillis ?: now) - startMillis).coerceAtLeast(0)
}
