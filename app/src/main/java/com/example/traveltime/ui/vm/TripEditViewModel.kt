package com.example.traveltime.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltime.data.AppContainer
import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripEditState(
    val loaded: Boolean = false,
    val original: Trip? = null,
    val mode: TravelMode = TravelMode.VEHICLE,
    val startMillis: Long = 0,
    val endMillis: Long? = null,
    val note: String = "",
) {
    val isValid: Boolean get() = endMillis == null || endMillis > startMillis
}

class TripEditViewModel(private val container: AppContainer, tripId: Long) : ViewModel() {
    private val _state = MutableStateFlow(TripEditState())
    val state: StateFlow<TripEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = container.repository.get(tripId)
            _state.value = if (trip == null) {
                TripEditState(loaded = true)
            } else {
                TripEditState(true, trip, trip.mode, trip.startMillis, trip.endMillis, trip.note)
            }
        }
    }

    fun setMode(mode: TravelMode) = _state.update { it.copy(mode = mode) }
    fun setNote(note: String) = _state.update { it.copy(note = note) }
    fun setStart(millis: Long) = _state.update { it.copy(startMillis = millis) }
    fun setEnd(millis: Long) = _state.update { it.copy(endMillis = millis) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val original = s.original ?: return
        if (!s.isValid) return
        viewModelScope.launch {
            container.repository.update(
                original.copy(mode = s.mode, startMillis = s.startMillis, endMillis = s.endMillis, note = s.note.trim())
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val original = _state.value.original ?: return
        viewModelScope.launch {
            container.repository.delete(original)
            if (original.isOngoing) container.notifier.cancel()
            onDone()
        }
    }

    private inline fun MutableStateFlow<TripEditState>.update(f: (TripEditState) -> TripEditState) {
        value = f(value)
    }

    companion object {
        fun factory(tripId: Long) = containerFactory { TripEditViewModel(it, tripId) }
    }
}
