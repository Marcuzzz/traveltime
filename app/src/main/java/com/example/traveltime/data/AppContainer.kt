package com.example.traveltime.data

import android.content.Context
import com.example.traveltime.tracking.TrackingConfig
import com.example.traveltime.tracking.TrackingManager
import com.example.traveltime.tracking.TripNotifier
import com.example.traveltime.tracking.TripTracker

class AppContainer(appContext: Context) {
    val context: Context = appContext.applicationContext
    val settings = SettingsStore(context)
    val repository = TripRepository(AppDatabase.open(context).tripDao())
    val trackingManager = TrackingManager(context)
    val notifier = TripNotifier(context, settings)
    val tracker = TripTracker(
        store = repository,
        config = {
            val s = settings.current
            TrackingConfig(
                enabledModes = s.enabledModes,
                minDurationMillis = s.minDurationMinutes * 60_000L,
                mergeGapMillis = s.mergeGapMinutes * 60_000L,
            )
        },
        listener = notifier,
    )

    /** Registers or removes activity-transition updates to match the current settings. */
    fun applyTracking() {
        val s = settings.current
        if (s.trackingEnabled) trackingManager.register(s.enabledModes) else trackingManager.unregister()
    }
}
