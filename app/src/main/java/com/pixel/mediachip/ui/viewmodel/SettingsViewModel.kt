package com.pixel.mediachip.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.components.ChipPreset
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI event representation for asynchronous snackbar notices and feedback.
 */
sealed class SettingsUiEvent {
    data class ShowSnackbar(val message: String) : SettingsUiEvent()
}

/**
 * Material 3 Expressive ViewModel managing reactive settings state and system integration.
 *
 * Responsibilities:
 * - Reactive [StateFlow] publishing immutable [ChipSettingsState]
 * - Bi-directional synchronization with [ChipSettings]
 * - Persistence via [ChipSettings.save], which updates SharedPreferences, invokes ContentProvider IPC,
 *   and broadcasts ACTION_SETTINGS_CHANGED to com.android.systemui
 * - One-click preset switching
 * - Root SystemUI restart dispatch
 * - Diagnostics telemetry for root, audio, and overlay permissions
 *
 * Zero hardcoded colors, zero emojis.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _settingsState = MutableStateFlow(ChipSettingsState())
    val settingsState: StateFlow<ChipSettingsState> = _settingsState.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        SystemDiagnostics(
            isRootAvailable = false,
            isRecordAudioGranted = false,
            isOverlayGranted = false
        )
    )
    val diagnostics: StateFlow<SystemDiagnostics> = _diagnostics.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

    init {
        loadSettings()
        refreshDiagnostics()
    }

    /**
     * Loads settings from [ChipSettings] (ContentProvider or SharedPreferences).
     */
    fun loadSettings() {
        val context = getApplication<Application>()
        val chipSettings = ChipSettings.get()
        chipSettings.load(context)
        _settingsState.value = chipSettings.toState()
        // Proactively broadcast loaded settings to SystemUI so state is immediately synchronized
        chipSettings.sendBroadcast(context)
    }

    /**
     * Updates in-memory Compose state immediately (for smooth 60/120fps slider dragging).
     */
    fun updateState(transform: (ChipSettingsState) -> ChipSettingsState) {
        _settingsState.value = transform(_settingsState.value)
    }

    /**
     * Commits current in-memory state to persistent storage, ContentProvider IPC, and SystemUI broadcast.
     */
    fun commitSettings() {
        persistSettings(_settingsState.value)
    }

    /**
     * Atomically updates state and commits to persistence (used by switches, buttons, and presets).
     */
    fun updateAndCommit(transform: (ChipSettingsState) -> ChipSettingsState) {
        val newState = transform(_settingsState.value)
        _settingsState.value = newState
        persistSettings(newState)
    }

    /**
     * Applies one of the 4 defined presets and commits immediately.
     */
    fun applyPreset(preset: ChipPreset) {
        updateAndCommit { it.applyPreset(preset) }
    }

    /**
     * Resets all parameters to default Pixel configuration and commits immediately.
     */
    fun resetToDefaults() {
        updateAndCommit { it.resetToDefaults() }
    }

    /**
     * Persists [state] to [ChipSettings], which flushes to SharedPreferences,
     * ContentProvider IPC (`saveSettings`), and broadcasts `ACTION_SETTINGS_CHANGED` to `com.android.systemui`.
     */
    private fun persistSettings(state: ChipSettingsState) {
        val context = getApplication<Application>()
        val chipSettings = ChipSettings.get()
        state.applyTo(chipSettings)
        chipSettings.save(context)
    }

    /**
     * Dispatches root SystemUI restart (`su -c "killall com.android.systemui"`).
     */
    fun restartSystemUi() {
        viewModelScope.launch {
            val result = RootActionHelper.restartSystemUi()
            result.fold(
                onSuccess = {
                    _events.emit(SettingsUiEvent.ShowSnackbar("SystemUI restarted successfully"))
                },
                onFailure = { error ->
                    val message = error.message ?: "Failed to restart SystemUI (Root denied)"
                    _events.emit(SettingsUiEvent.ShowSnackbar(message))
                }
            )
        }
    }

    /**
     * Refreshes system diagnostics for Root, RECORD_AUDIO, and SYSTEM_ALERT_WINDOW permissions.
     */
    fun refreshDiagnostics() {
        viewModelScope.launch {
            val diag = RootActionHelper.getDiagnostics(getApplication())
            _diagnostics.value = diag
        }
    }
}
