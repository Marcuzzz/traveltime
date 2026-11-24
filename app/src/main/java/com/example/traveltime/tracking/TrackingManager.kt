package com.example.traveltime.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.traveltime.data.TravelMode
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/** Registers/unregisters for Activity Recognition transition updates. */
class TrackingManager(private val context: Context) {

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** Requests updates for [modes]; replaces any earlier registration. */
    @SuppressLint("MissingPermission")
    fun register(modes: Set<TravelMode>) {
        if (!hasPermission()) return
        if (modes.isEmpty()) {
            unregister()
            return
        }
        val transitions = modes.flatMap { mode ->
            listOf(
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
            ).map { type ->
                ActivityTransition.Builder()
                    .setActivityType(mode.toActivityType())
                    .setActivityTransition(type)
                    .build()
            }
        }
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent())
            .addOnFailureListener { Log.w(TAG, "Transition registration failed", it) }
    }

    @SuppressLint("MissingPermission")
    fun unregister() {
        if (!hasPermission()) return
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(pendingIntent())
            .addOnFailureListener { Log.w(TAG, "Transition removal failed", it) }
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
            .setAction(ActivityTransitionReceiver.ACTION)
        // Must be mutable: Play services fills in the transition result.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        private const val TAG = "TrackingManager"

        fun TravelMode.toActivityType(): Int = when (this) {
            TravelMode.VEHICLE -> DetectedActivity.IN_VEHICLE
            TravelMode.BICYCLE -> DetectedActivity.ON_BICYCLE
            TravelMode.RUNNING -> DetectedActivity.RUNNING
            TravelMode.WALKING -> DetectedActivity.WALKING
        }

        fun modeForActivityType(type: Int): TravelMode? =
            TravelMode.entries.firstOrNull { it.toActivityType() == type }
    }
}
