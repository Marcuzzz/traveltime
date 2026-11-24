package com.example.traveltime.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.traveltime.data.TravelMode
import java.time.format.DateTimeFormatter

val TravelMode.icon: ImageVector
    get() = when (this) {
        TravelMode.VEHICLE -> Icons.Filled.DirectionsCar
        TravelMode.BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        TravelMode.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
        TravelMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    }

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")

/** Runtime permissions needed for tracking + the progress notification. */
fun trackingPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()
