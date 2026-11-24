package com.example.traveltime.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val trackingEnabled: Boolean = false,
    val enabledModes: Set<TravelMode> = setOf(TravelMode.VEHICLE, TravelMode.BICYCLE),
    val notificationsEnabled: Boolean = true,
    /** Auto-detected trips shorter than this are discarded (filters false positives). */
    val minDurationMinutes: Int = 3,
    /** A stop shorter than this (traffic light, train change) continues the same trip. */
    val mergeGapMinutes: Int = 5,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putBoolean(KEY_TRACKING, next.trackingEnabled)
            .putStringSet(KEY_MODES, next.enabledModes.map { it.name }.toSet())
            .putBoolean(KEY_NOTIFICATIONS, next.notificationsEnabled)
            .putInt(KEY_MIN_DURATION, next.minDurationMinutes)
            .putInt(KEY_MERGE_GAP, next.mergeGapMinutes)
            .apply()
        _settings.value = next
    }

    private fun load(): AppSettings {
        val d = AppSettings()
        val modes = prefs.getStringSet(KEY_MODES, null)
            ?.mapNotNull { name -> TravelMode.entries.firstOrNull { it.name == name } }
            ?.toSet()
        return AppSettings(
            trackingEnabled = prefs.getBoolean(KEY_TRACKING, d.trackingEnabled),
            enabledModes = modes ?: d.enabledModes,
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, d.notificationsEnabled),
            minDurationMinutes = prefs.getInt(KEY_MIN_DURATION, d.minDurationMinutes),
            mergeGapMinutes = prefs.getInt(KEY_MERGE_GAP, d.mergeGapMinutes),
        )
    }

    private companion object {
        const val KEY_TRACKING = "tracking_enabled"
        const val KEY_MODES = "enabled_modes"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_MIN_DURATION = "min_duration_minutes"
        const val KEY_MERGE_GAP = "merge_gap_minutes"
    }
}
