package com.camara.app.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.camara.app.BuildConfig
import com.camara.app.settings.Contact

/**
 * Diagnostics (Rule 7). Collection is on by default; the user can turn it off and still
 * export or clear whatever has already been collected.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    vm: DiagnosticsViewModel = viewModel(),
) {
    var showFormats by remember { mutableStateOf(false) }
    var format by remember { mutableStateOf(LogExporter.Format.TXT) }

    LaunchedEffect(Unit) { vm.refresh() }

    Text("Diagnostics", style = MaterialTheme.typography.titleMedium)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Collect logs")
            Text(
                "On by default, so a crash or a bug can be diagnosed without reproducing it first.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }

    Text(
        "Collected: ${"%.1f".format(vm.logBytes / 1024.0)} KB",
        style = MaterialTheme.typography.bodySmall,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                format = LogExporter.Format.TXT
                vm.exportAndShare(LogExporter.Format.TXT, "Camara", BuildConfig.VERSION_NAME)
            }
        ) { Text("Share logs") }

        OutlinedButton(onClick = { showFormats = true }) { Text("Export…") }
        OutlinedButton(onClick = { vm.clear() }) { Text("Clear") }
    }

    vm.message?.let {
        Text(it, style = MaterialTheme.typography.bodySmall)
    }

    Text(
        "If logs are empty, nothing has happened yet — collection only records from the " +
            "moment it is on.",
        style = MaterialTheme.typography.bodySmall,
    )

    Text(
        "Still stuck? Message @${Contact.INSTAGRAM_HANDLE} with the exported file.",
        style = MaterialTheme.typography.bodySmall,
    )

    if (showFormats) {
        AlertDialog(
            onDismissRequest = { showFormats = false },
            title = { Text("Export format") },
            text = {
                Column {
                    LogExporter.Format.entries.forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = {
                                format = f
                                showFormats = false
                                vm.exportAndShare(f, "Camara", BuildConfig.VERSION_NAME)
                            },
                            label = { Text(f.label) },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormats = false }) { Text("Cancel") }
            },
        )
    }
}
