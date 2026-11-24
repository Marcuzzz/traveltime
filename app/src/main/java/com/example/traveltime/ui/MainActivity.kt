package com.example.traveltime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.traveltime.ui.nav.TravelTimeNavHost
import com.example.traveltime.ui.theme.TravelTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelTimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TravelTimeNavHost()
                }
            }
        }
    }
}
