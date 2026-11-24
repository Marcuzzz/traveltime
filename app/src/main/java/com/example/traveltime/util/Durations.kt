package com.example.traveltime.util

/** "45 s", "12 min", "1 h 05 min". */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000)
    if (totalSeconds < 60) return "$totalSeconds s"
    val totalMinutes = totalSeconds / 60
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "$hours h ${minutes.toString().padStart(2, '0')} min"
}

/** "0:42:07" style, for a live running timer. */
fun formatClock(millis: Long): String {
    val s = millis.coerceAtLeast(0) / 1000
    return "%d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
}
