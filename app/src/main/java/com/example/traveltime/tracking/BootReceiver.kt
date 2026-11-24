package com.example.traveltime.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.traveltime.TravelTimeApp

/** Transition registrations don't survive a reboot or app update; restore them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                (context.applicationContext as TravelTimeApp).container.applyTracking()
        }
    }
}
