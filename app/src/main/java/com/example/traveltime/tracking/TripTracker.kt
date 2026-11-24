package com.example.traveltime.tracking

import com.example.traveltime.data.Trip
import com.example.traveltime.data.TripStore
import com.example.traveltime.data.TravelMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TrackingConfig(
    val enabledModes: Set<TravelMode>,
    val minDurationMillis: Long,
    val mergeGapMillis: Long,
)

interface TripListener {
    fun onTripStarted(trip: Trip) {}
    fun onTripEnded(trip: Trip) {}
}

/**
 * Turns activity transitions ("entered vehicle", "exited vehicle", ...) into trips.
 *
 * Rules:
 * - Entering an enabled mode starts a trip, unless one of that mode is already running.
 * - Entering a different mode while an automatic trip runs ends that trip and starts a new one.
 * - If the previous trip had the same mode and ended less than `mergeGap` ago, it is resumed
 *   instead (so a red light or a quick stop doesn't split the journey).
 * - Exiting the mode of the running automatic trip ends it.
 * - Manually started trips are never touched by detection.
 * - Automatic trips shorter than `minDuration` are pruned once they can no longer be resumed.
 */
class TripTracker(
    private val store: TripStore,
    private val config: () -> TrackingConfig,
    private val listener: TripListener = object : TripListener {},
) {
    private val mutex = Mutex()

    suspend fun onTransition(mode: TravelMode, entered: Boolean, atMillis: Long) = mutex.withLock {
        val cfg = config()
        store.pruneShort(atMillis - cfg.mergeGapMillis, cfg.minDurationMillis)
        val ongoing = store.ongoing()

        if (!entered) {
            if (ongoing != null && ongoing.auto && ongoing.mode == mode) {
                endTrip(ongoing, atMillis)
            }
            return@withLock
        }

        if (mode !in cfg.enabledModes) return@withLock
        if (ongoing != null) {
            if (!ongoing.auto || ongoing.mode == mode) return@withLock
            endTrip(ongoing, atMillis)
        }

        val last = store.lastFinished()
        val lastEnd = last?.endMillis
        if (last != null && lastEnd != null && last.auto && last.mode == mode &&
            atMillis >= lastEnd && atMillis - lastEnd <= cfg.mergeGapMillis
        ) {
            val resumed = last.copy(endMillis = null)
            store.update(resumed)
            listener.onTripStarted(resumed)
        } else {
            val trip = Trip(mode = mode, startMillis = atMillis, auto = true)
            listener.onTripStarted(trip.copy(id = store.insert(trip)))
        }
    }

    /** Starts a manual trip, ending whatever is running. */
    suspend fun startManual(mode: TravelMode, nowMillis: Long): Trip = mutex.withLock {
        store.ongoing()?.let { endTrip(it, nowMillis) }
        val trip = Trip(mode = mode, startMillis = nowMillis, auto = false)
        trip.copy(id = store.insert(trip)).also(listener::onTripStarted)
    }

    /** Ends the running trip, if any (manual or automatic). */
    suspend fun stopOngoing(nowMillis: Long) = mutex.withLock {
        store.ongoing()?.let { endTrip(it, nowMillis) }
    }

    /** Removes short auto trips that can no longer be resumed. */
    suspend fun prune(nowMillis: Long) = mutex.withLock {
        val cfg = config()
        store.pruneShort(nowMillis - cfg.mergeGapMillis, cfg.minDurationMillis)
    }

    private suspend fun endTrip(trip: Trip, atMillis: Long) {
        val ended = trip.copy(endMillis = maxOf(atMillis, trip.startMillis))
        store.update(ended)
        listener.onTripEnded(ended)
    }
}
