package com.camara.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.camara.app.BuildConfig
import com.camara.app.diagnostics.DiagnosticsSection
import androidx.compose.runtime.remember
import com.camara.app.security.AppLock
import com.camara.app.updater.UpdateUiState
import com.camara.app.updater.UpdateViewModel

private val ACCENTS = listOf(
    "Pink" to 0xFFFF2D78L,
    "Purple" to 0xFF6750A4L,
    "Blue" to 0xFF1565C0L,
    "Green" to 0xFF2E7D32L,
    "Orange" to 0xFFEF6C00L,
)

private val LANGUAGES = listOf("system" to "System", "en" to "English", "hi" to "हिन्दी")
private val TIMERS = listOf(0, 3, 5, 10)

/**
 * Rich, fully-wired Settings screen. Every control writes through the view model to
 * DataStore and reads back from the same Flow, so it persists and takes effect
 * immediately (Rule 4). Ends with the version + tappable @osintgram_io contact, and
 * carries the in-app update control (Rule 6) and Diagnostics (Rule 7).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
    updater: UpdateViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---------------------------------------------------------- Appearance
            Section("Appearance")

            Text("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Text("Accent colour")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ACCENTS.forEach { (name, argb) ->
                    FilterChip(
                        selected = state.accentColor == argb,
                        onClick = { vm.setAccentColor(argb) },
                        label = { Text(name) },
                    )
                }
            }

            Text("Text size (${"%.2f".format(state.textScale)}x)")
            Slider(
                value = state.textScale,
                onValueChange = vm::setTextScale,
                valueRange = 0.85f..1.5f,
            )

            Text("Density (${"%.2f".format(state.density)}x)")
            Slider(
                value = state.density,
                onValueChange = vm::setDensity,
                valueRange = 0.80f..1.20f,
            )

            // ------------------------------------------------------------- General
            Section("General")

            Text("Language")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LANGUAGES.forEach { (code, label) ->
                    FilterChip(
                        selected = state.language == code,
                        onClick = { vm.setLanguage(code) },
                        label = { Text(label) },
                    )
                }
            }

            ToggleRow(
                title = "Notifications",
                subtitle = "Tips and release announcements",
                checked = state.notificationsEnabled,
                onChange = vm::setNotifications,
            )

            ToggleRow(
                title = "Full-screen mode",
                subtitle = "Hide the status and navigation bars while using Camara",
                checked = state.immersiveMode,
                onChange = vm::setImmersive,
            )

            ToggleRow(
                title = "Mirror selfie",
                subtitle = "Save front-camera photos the way you see them",
                checked = state.mirrorSelfie,
                onChange = vm::setMirrorSelfie,
            )

            ToggleRow(
                title = "Grid by default",
                subtitle = "Show the rule-of-thirds grid when the camera opens",
                checked = state.gridByDefault,
                onChange = vm::setGridByDefault,
            )

            // Only offered when the device can actually authenticate. A toggle that does
            // nothing is worse than an explanation.
            val lockAvailable = remember { AppLock.isAvailable(context) }
            if (lockAvailable) {
                ToggleRow(
                    title = "App lock",
                    subtitle = "Ask for your fingerprint or device PIN before showing anything",
                    checked = state.appLockEnabled,
                    onChange = vm::setAppLock,
                )
            } else {
                Text(
                    "App lock needs a fingerprint or a screen lock set up on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Default timer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TIMERS.forEach { seconds ->
                    FilterChip(
                        selected = state.timerSeconds == seconds,
                        onClick = { vm.setTimerSeconds(seconds) },
                        label = { Text(if (seconds == 0) "Off" else "${seconds}s") },
                    )
                }
            }

            // ------------------------------------------------------------ Features
            Section("Features")
            Text(
                "Switch off anything you don't use — the control disappears from the camera.",
                style = MaterialTheme.typography.bodySmall,
            )
            FeatureFlag.entries.forEach { flag ->
                ToggleRow(
                    title = flag.label,
                    subtitle = null,
                    checked = state.isEnabled(flag),
                    onChange = { vm.setFeatureFlag(flag.id, it) },
                )
            }

            // ------------------------------------------------------------- Updates
            // Compiled out of the Play build: Play does not allow an app to install its
            // own updates, so that build has no updater and no install-packages
            // permission to go with it.
            if (BuildConfig.SELF_UPDATE_ENABLED) {
                Section("Updates")

                val updateState = updater.state
                Text("Installed version ${BuildConfig.VERSION_NAME}")

                when (updateState) {
                    UpdateUiState.Checking -> Text("Checking for updates…")
                    UpdateUiState.UpToDate -> Text("You're on the latest version.")
                    is UpdateUiState.Available -> {
                        Text("Version ${updateState.info.latestVersion} is available.")
                        Button(onClick = { updater.install(updateState.info) }) { Text("Update now") }
                    }
                    UpdateUiState.Downloading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Downloading update…")
                    }
                    is UpdateUiState.Failed -> Text("Update failed: ${updateState.message}")
                    UpdateUiState.Idle -> Unit
                }

                OutlinedButton(
                    onClick = { updater.check(BuildConfig.VERSION_NAME) },
                    enabled = updateState !is UpdateUiState.Checking &&
                        updateState !is UpdateUiState.Downloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Check for updates")
                }
                Text(
                    "Downloads the signed APK and installs it here — no browser needed.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ---------------------------------------------------------- Diagnostics
            DiagnosticsSection(
                enabled = state.diagnosticsEnabled,
                onEnabledChange = vm::setDiagnosticsEnabled,
            )

            // --------------------------------------------------------------- Reset
            Section("Reset")
            OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset to defaults")
            }

            HorizontalDivider()

            // ------------------------------------------- Footer: version + contact
            Text(
                "Camara ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "@${Contact.INSTAGRAM_HANDLE}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { Contact.openInstagram(context) },
            )
            Text(
                "Suggestions, feedback or a feature request? Tap the handle above.",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(24.dp))
        }

        ExploreMoreAppsSection()
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
