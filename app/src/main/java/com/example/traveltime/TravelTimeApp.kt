package com.example.traveltime

import android.app.Application
import com.example.traveltime.data.AppContainer

class TravelTimeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applyTracking()
    }
}
