package com.mobilegamecontroller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilegamecontroller.data.repository.ControllerRepository
import com.mobilegamecontroller.domain.model.AnalogStick
import com.mobilegamecontroller.domain.model.ConnectionHistory
import com.mobilegamecontroller.domain.model.ConnectionState
import com.mobilegamecontroller.domain.model.ConnectUiState
import com.mobilegamecontroller.domain.model.ControllerButton
import com.mobilegamecontroller.domain.model.ControllerSettings
import com.mobilegamecontroller.domain.model.ControllerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val repository: ControllerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.connectionHistory.collect { history ->
                _uiState.update { it.copy(recentConnections = history) }
            }
        }
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        isConnecting = state == ConnectionState.CONNECTING
                    )
                }
            }
        }
    }

    fun updateIpAddress(ip: String) {
        _uiState.update { it.copy(ipAddress = ip, errorMessage = null) }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port, errorMessage = null) }
    }

    fun connect(onSuccess: () -> Unit) {
        val state = _uiState.value
        val ip = state.ipAddress.trim()
        val portStr = state.port.trim()

        if (!isValidIp(ip)) {
            _uiState.update { it.copy(errorMessage = "Invalid IP address") }
            return
        }
        val port = portStr.toIntOrNull()
        if (port == null || port !in 1..65535) {
            _uiState.update { it.copy(errorMessage = "Port must be between 1 and 65535") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            val result = repository.connect(ip, port, viewModelScope)
            _uiState.update { it.copy(isConnecting = false) }
            if (result.isSuccess) {
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Connection failed")
                }
            }
        }
    }

    fun connectFromHistory(history: ConnectionHistory, onSuccess: () -> Unit) {
        _uiState.update {
            it.copy(ipAddress = history.ipAddress, port = history.port.toString())
        }
        connect(onSuccess)
    }

    private fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull() ?: return false
            num in 0..255
        }
    }
}

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val repository: ControllerRepository
) : ViewModel() {

    val uiState: StateFlow<ControllerUiState> = combine(
        repository.connectionState,
        repository.latencyMs,
        repository.settings
    ) { connectionState, latency, settings ->
        ControllerUiState(
            connectionState = connectionState,
            latencyMs = latency,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ControllerUiState()
    )

    fun onButtonEvent(button: ControllerButton, pressed: Boolean) {
        viewModelScope.launch {
            repository.sendButton(button, pressed)
        }
    }

    fun onAnalogEvent(stick: AnalogStick, x: Float, y: Float) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            repository.sendAnalog(stick, x, y, settings)
        }
    }

    fun disconnect(onDisconnected: () -> Unit) {
        repository.disconnect()
        onDisconnected()
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ControllerRepository
) : ViewModel() {

    val settings: StateFlow<ControllerSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ControllerSettings()
    )

    fun updateSettings(settings: ControllerSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }
}
