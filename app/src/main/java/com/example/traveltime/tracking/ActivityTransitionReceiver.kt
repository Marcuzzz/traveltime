package com.example.traveltime.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.traveltime.TravelTimeApp
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Receives transition events from Play services, even when the app isn't running. */
class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val container = (context.applicationContext as TravelTimeApp).container
        if (!container.settings.current.trackingEnabled) return

        val pending = goAsync()
        scope.launch {
            try {
                // Event times are elapsed-realtime; convert to wall clock.
                val nowWall = System.currentTimeMillis()
                val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
                result.transitionEvents
                    .sortedBy { it.elapsedRealTimeNanos }
                    .forEach { event ->
                        val mode = TrackingManager.modeForActivityType(event.activityType) ?: return@forEach
                        val at = nowWall - (nowElapsedNanos - event.elapsedRealTimeNanos) / 1_000_000
                        container.tracker.onTransition(
                            mode = mode,
                            entered = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                            atMillis = at,
                        )
                    }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.example.traveltime.ACTIVITY_TRANSITION"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
