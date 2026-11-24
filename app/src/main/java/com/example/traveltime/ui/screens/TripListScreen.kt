package com.example.traveltime.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveltime.data.AppSettings
import com.example.traveltime.data.TravelMode
import com.example.traveltime.data.Trip
import com.example.traveltime.ui.vm.TripListViewModel
import com.example.traveltime.util.CsvExport
import com.example.traveltime.util.formatClock
import com.example.traveltime.util.formatDuration
import com.example.traveltime.util.groupByDay
import com.example.traveltime.util.startOfDay
import com.example.traveltime.util.startOfMonth
import com.example.traveltime.util.startOfWeek
import com.example.traveltime.util.totalInRange
import com.example.traveltime.util.totalsByMode
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    vm: TripListViewModel = viewModel(factory = TripListViewModel.Factory),
) {
    val context = LocalContext.current
    val trips by vm.trips.collectAsState()
    val settings by vm.settings.collectAsState()
    val ongoing = trips.firstOrNull { it.isOngoing }
    val zone = remember { ZoneId.systemDefault() }

    // Re-check permission each time the screen resumes (user may grant it in system settings).
    var permissionCheck by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionCheck++ }
    val hasPermission = remember(permissionCheck) { vm.hasActivityPermission() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionCheck++
        if (vm.hasActivityPermission()) vm.enableTracking()
    }

    // Clock: tick every second while a trip runs, otherwise once a minute.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ongoing != null) {
        while (true) {
            now = System.currentTimeMillis()
            delay(if (ongoing != null) 1_000 else 60_000)
        }
    }

    var shareMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TravelTime") },
                actions = {
                    Box {
                        IconButton(onClick = { shareMenu = true }, enabled = trips.isNotEmpty()) {
                            Icon(Icons.Filled.Share, contentDescription = "Share as CSV")
                        }
                        DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                            val today = LocalDate.now(zone)
                            listOf(
                                "This week" to startOfWeek(today, zone),
                                "This month" to startOfMonth(today, zone),
                                "All trips" to Long.MIN_VALUE,
                            ).forEach { (label, from) ->
                                DropdownMenuItem(
                                    text = { Text("Share CSV: $label") },
                                    onClick = {
                                        shareMenu = false
                                        val selected = trips.filter { (it.endMillis ?: now) > from }
                                        CsvExport.share(context, selected, label.lowercase())
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!settings.trackingEnabled || !hasPermission) {
                item {
                    SetupCard(
                        settings = settings,
                        hasPermission = hasPermission,
                        onEnable = {
                            val perms = trackingPermissions()
                            if (perms.isEmpty() || hasPermission) vm.enableTracking()
                            else permissionLauncher.launch(perms)
                        },
                    )
                }
            }
            item {
                CurrentTripCard(
                    ongoing = ongoing,
                    now = now,
                    onStart = vm::startManual,
                    onStop = vm::stopOngoing,
                )
            }
            item { SummaryCard(trips, now, zone) }

            val days = groupByDay(trips, zone)
            if (days.isEmpty()) {
                item {
                    Text(
                        "No trips yet. Once tracking is on, trips appear here automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            days.forEach { (day, dayTrips) ->
                item(key = "day-$day") {
                    val total = dayTrips.sumOf { it.durationMillis(now) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(day.format(dayFormatter), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(formatDuration(total), style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(dayTrips, key = { it.id }) { trip ->
                    TripRow(trip, now, zone, onClick = { onOpen(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun SetupCard(settings: AppSettings, hasPermission: Boolean, onEnable: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Automatic tracking is off", style = MaterialTheme.typography.titleMedium)
            Text(
                if (settings.trackingEnabled && !hasPermission) {
                    "Physical activity permission was revoked. Grant it again to keep detecting trips."
                } else {
                    "TravelTime uses Android's activity recognition to notice when you start and stop " +
                        "travelling. It doesn't use GPS and works with the app closed."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onEnable) { Text("Turn on tracking") }
        }
    }
}

@Composable
private fun CurrentTripCard(
    ongoing: Trip?,
    now: Long,
    onStart: (TravelMode) -> Unit,
    onStop: () -> Unit,
) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ongoing != null) {
                Icon(ongoing.mode.icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(formatClock(ongoing.durationMillis(now)), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${ongoing.mode.label} · ${if (ongoing.auto) "detected" else "manual"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FilledTonalButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            } else {
                var menu by remember { mutableStateOf(false) }
                Text("Not travelling", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Box {
                    FilledTonalButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Start trip")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        TravelMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                leadingIcon = { Icon(mode.icon, contentDescription = null) },
                                onClick = { menu = false; onStart(mode) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(trips: List<Trip>, now: Long, zone: ZoneId) {
    val today = LocalDate.now(zone)
    val dayStart = startOfDay(today, zone)
    val weekStart = startOfWeek(today, zone)
    val monthStart = startOfMonth(today, zone)
    val end = Long.MAX_VALUE
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Stat("Today", totalInRange(trips, dayStart, end, now), Modifier.weight(1f))
                Stat("This week", totalInRange(trips, weekStart, end, now), Modifier.weight(1f))
                Stat("This month", totalInRange(trips, monthStart, end, now), Modifier.weight(1f))
            }
            val byMode = totalsByMode(trips, weekStart, end, now)
            if (byMode.isNotEmpty()) {
                HorizontalDivider()
                Text("This week by mode", style = MaterialTheme.typography.labelMedium)
                byMode.entries.sortedByDescending { it.value }.forEach { (mode, ms) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(mode.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, modifier = Modifier.weight(1f))
                        Text(formatDuration(ms))
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, millis: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatDuration(millis), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TripRow(trip: Trip, now: Long, zone: ZoneId, onClick: () -> Unit) {
    val start = Instant.ofEpochMilli(trip.startMillis).atZone(zone)
    val endText = trip.endMillis?.let { Instant.ofEpochMilli(it).atZone(zone).format(timeFormatter) } ?: "now"
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(trip.mode.icon, contentDescription = trip.mode.label)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("${start.format(timeFormatter)} – $endText", style = MaterialTheme.typography.bodyLarge)
            val sub = buildList {
                add(trip.mode.label)
                if (!trip.auto) add("manual")
                if (trip.note.isNotBlank()) add(trip.note)
            }.joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(formatDuration(trip.durationMillis(now)), style = MaterialTheme.typography.bodyLarge)
    }
}
