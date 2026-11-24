package com.example.traveltime.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveltime.data.TravelMode
import com.example.traveltime.ui.vm.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by vm.settings.collectAsState()
    var confirmDeleteAll by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { if (vm.hasActivityPermission()) vm.setTracking(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SwitchRow(
                title = "Automatic tracking",
                subtitle = "Detect trips with activity recognition",
                checked = settings.trackingEnabled,
                onChange = { enabled ->
                    val perms = trackingPermissions()
                    if (enabled && !vm.hasActivityPermission() && perms.isNotEmpty()) permissionLauncher.launch(perms)
                    else vm.setTracking(enabled)
                },
            )
            SwitchRow(
                title = "Trip notification",
                subtitle = "Show a running timer while travelling",
                checked = settings.notificationsEnabled,
                onChange = vm::setNotifications,
            )

            HorizontalDivider()
            Text("Detect these modes", style = MaterialTheme.typography.titleSmall)
            TravelMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = mode in settings.enabledModes,
                        onCheckedChange = { vm.toggleMode(mode, it) },
                    )
                    Icon(mode.icon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(mode.label)
                }
            }
            Text(
                "Walking picks up every stroll to the shop; leave it off if you only care about commutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            MinutesSlider(
                title = "Ignore trips shorter than",
                value = settings.minDurationMinutes,
                range = 0..15,
                onChange = vm::setMinDuration,
            )
            MinutesSlider(
                title = "Join trips with stops shorter than",
                value = settings.mergeGapMinutes,
                range = 0..20,
                onChange = vm::setMergeGap,
            )

            HorizontalDivider()
            OutlinedButton(onClick = { confirmDeleteAll = true }) { Text("Delete all trips") }
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all trips?") },
            text = { Text("This can't be undone. Share a CSV first if you want a copy.") },
            confirmButton = {
                TextButton(onClick = { confirmDeleteAll = false; vm.deleteAll() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MinutesSlider(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text("$title: $value min", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}
