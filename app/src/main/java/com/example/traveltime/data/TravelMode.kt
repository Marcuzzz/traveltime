package com.example.traveltime.data

/** How the user was travelling. Stored by name in the database. */
enum class TravelMode(val label: String) {
    VEHICLE("Vehicle"),
    BICYCLE("Bicycle"),
    RUNNING("Running"),
    WALKING("Walking"),
}
