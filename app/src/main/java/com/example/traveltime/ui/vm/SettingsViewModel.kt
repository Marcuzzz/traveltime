package com.example.traveltime.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltime.data.AppContainer
import com.example.traveltime.data.AppSettings
import com.example.traveltime.data.TravelMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settings.settings

    fun hasActivityPermission(): Boolean = container.trackingManager.hasPermission()

    fun setTracking(enabled: Boolean) = updateAndApply { it.copy(trackingEnabled = enabled) }

    fun toggleMode(mode: TravelMode, enabled: Boolean) = updateAndApply {
        it.copy(enabledModes = if (enabled) it.enabledModes + mode else it.enabledModes - mode)
    }

    fun setNotifications(enabled: Boolean) {
        container.settings.update { it.copy(notificationsEnabled = enabled) }
        if (!enabled) container.notifier.cancel()
    }

    fun setMinDuration(minutes: Int) = container.settings.update { it.copy(minDurationMinutes = minutes) }
    fun setMergeGap(minutes: Int) = container.settings.update { it.copy(mergeGapMinutes = minutes) }

    fun deleteAll() {
        viewModelScope.launch {
            container.repository.deleteAll()
            container.notifier.cancel()
        }
    }

    private fun updateAndApply(transform: (AppSettings) -> AppSettings) {
        container.settings.update(transform)
        container.applyTracking()
    }

    companion object {
        val Factory = containerFactory { SettingsViewModel(it) }
    }
}
