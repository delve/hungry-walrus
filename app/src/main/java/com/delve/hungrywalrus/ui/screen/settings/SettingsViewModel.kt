package com.delve.hungrywalrus.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.delve.hungrywalrus.util.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

data class SettingsUiState(
    val hasKey: Boolean = false,
    val keyMasked: String = "",
)

sealed interface SettingsUiEvent {
    data object KeySaved : SettingsUiEvent
    data object KeyCleared : SettingsUiEvent
    data class ReadError(val message: String) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refreshState()
    }

    private fun refreshState() {
        val key = apiKeyStore.getApiKey()
        _uiState.value = SettingsUiState(
            hasKey = key != null,
            keyMasked = if (key != null) maskKey(key) else "",
        )
    }

    fun saveKey(key: String) {
        if (key.isBlank()) return
        apiKeyStore.saveApiKey(key.trim())
        refreshState()
        _events.trySend(SettingsUiEvent.KeySaved)
    }

    fun clearKey() {
        apiKeyStore.clearApiKey()
        refreshState()
        _events.trySend(SettingsUiEvent.KeyCleared)
    }

    private fun maskKey(key: String): String {
        return if (key.length > 8) {
            "****${key.takeLast(4)}"
        } else {
            "****"
        }
    }
}
