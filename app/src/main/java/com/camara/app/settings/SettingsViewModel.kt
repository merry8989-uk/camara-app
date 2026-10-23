package com.camara.app.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.camara.app.diagnostics.DiagnosticsLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val state: StateFlow<SettingsState> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setAccentColor(color: Long) = viewModelScope.launch { repo.setAccentColor(color) }
    fun setTextScale(scale: Float) = viewModelScope.launch { repo.setTextScale(scale) }
    fun setDensity(value: Float) = viewModelScope.launch { repo.setDensity(value) }
    fun setLanguage(language: String) = viewModelScope.launch { repo.setLanguage(language) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotifications(enabled) }
    fun setImmersive(enabled: Boolean) = viewModelScope.launch { repo.setImmersive(enabled) }
    fun setMirrorSelfie(enabled: Boolean) = viewModelScope.launch { repo.setMirrorSelfie(enabled) }
    fun setGridByDefault(enabled: Boolean) = viewModelScope.launch { repo.setGridByDefault(enabled) }
    fun setAppLock(enabled: Boolean) = viewModelScope.launch { repo.setAppLock(enabled) }
    fun setTimerSeconds(seconds: Int) = viewModelScope.launch { repo.setTimerSeconds(seconds) }
    fun setFeatureFlag(id: String, enabled: Boolean) =
        viewModelScope.launch { repo.setFeatureFlag(id, enabled) }

    fun setDiagnosticsEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.setDiagnosticsEnabled(enabled)
        DiagnosticsLogger.setEnabled(enabled)
    }

    fun reset() = viewModelScope.launch { repo.reset() }
}
