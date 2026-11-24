package com.example.traveltime.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltime.data.AppContainer
import com.example.traveltime.data.AppSettings
import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.Trip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripListViewModel(private val container: AppContainer) : ViewModel() {

    val trips: StateFlow<List<Trip>> = container.repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = container.settings.settings

    init {
        viewModelScope.launch { container.tracker.prune(System.currentTimeMillis()) }
    }

    fun hasActivityPermission(): Boolean = container.trackingManager.hasPermission()

    fun enableTracking() {
        container.settings.update { it.copy(trackingEnabled = true) }
        container.applyTracking()
    }

    fun startManual(mode: TravelMode) {
        viewModelScope.launch { container.tracker.startManual(mode, System.currentTimeMillis()) }
    }

    fun stopOngoing() {
        viewModelScope.launch { container.tracker.stopOngoing(System.currentTimeMillis()) }
    }

    companion object {
        val Factory = containerFactory { TripListViewModel(it) }
    }
}
