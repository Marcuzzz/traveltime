package com.example.traveltime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveltime.data.TravelMode
import com.example.traveltime.ui.vm.TripEditViewModel
import com.example.traveltime.util.formatDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditScreen(tripId: Long, onBack: () -> Unit) {
    val vm: TripEditViewModel = viewModel(factory = TripEditViewModel.factory(tripId))
    val state by vm.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (state.original != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete trip")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        if (state.original == null) {
            Text("This trip no longer exists.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Mode", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TravelMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { vm.setMode(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = { Icon(mode.icon, contentDescription = null) },
                    )
                }
            }

            DateTimeField("Start", state.startMillis, vm::setStart)
            val end = state.endMillis
            if (end != null) {
                DateTimeField("End", end, vm::setEnd)
                if (state.isValid) {
                    Text("Duration: ${formatDuration(end - state.startMillis)}", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("End must be after start.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text("This trip is still in progress.", style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("Note (e.g. commute, client visit)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.save(onBack) },
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trip?") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Shows a date and a time button; each opens a picker and reports the combined local date-time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(label: String, millis: Long, onChange: (Long) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val current = Instant.ofEpochMilli(millis).atZone(zone)
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDate = true }) { Text(current.format(dayFormatter)) }
            OutlinedButton(onClick = { showTime = true }) { Text(current.format(timeFormatter)) }
        }
    }

    if (showDate) {
        // DatePicker works in UTC midnight millis.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        onChange(combine(date, current.toLocalTime(), zone))
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }

    if (showTime) {
        val pickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(combine(current.toLocalDate(), LocalTime.of(pickerState.hour, pickerState.minute), zone))
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
        )
    }
}

private fun combine(date: LocalDate, time: LocalTime, zone: ZoneId): Long =
    date.atTime(time.withSecond(0).withNano(0)).atZone(zone).toInstant().toEpochMilli()
