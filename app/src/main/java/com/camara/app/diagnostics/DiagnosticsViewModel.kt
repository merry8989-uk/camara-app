package com.camara.app.diagnostics

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiagnosticsViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext

    var logBytes: Long by mutableStateOf(0L)
        private set

    var message: String? by mutableStateOf(null)
        private set

    fun refresh() {
        logBytes = DiagnosticsLogger.sizeBytes()
    }

    fun clear() {
        DiagnosticsLogger.clear()
        refresh()
        message = "Logs cleared"
    }

    fun exportAndShare(format: LogExporter.Format, appName: String, version: String) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                LogExporter.export(context, format, appName, version)
            }
            LogExporter.share(context, file, format.mime)
            message = "Exported ${file.name}"
        }
    }
}
