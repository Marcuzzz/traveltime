package com.example.traveltime.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.traveltime.TravelTimeApp
import com.example.traveltime.data.AppContainer

/** Builds a ViewModel from the app's [AppContainer]. */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer) -> VM) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TravelTimeApp
            @Suppress("UNCHECKED_CAST")
            return create(app.container) as T
        }
    }
