package com.mobilegamecontroller.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the current TCP connection state to the PC server.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * Xbox-style controller button identifiers matching the JSON protocol.
 */
enum class ControllerButton {
    A, B, X, Y,
    START, SELECT,
    LB, RB, LT, RT,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT
}

/**
 * Analog stick identifier for left/right sticks.
 */
enum class AnalogStick {
    LEFT, RIGHT
}

/**
 * User-configurable controller settings persisted locally.
 */
data class ControllerSettings(
    val analogSensitivity: Float = 1.0f,
    val analogDeadzone: Float = 0.12f,
    val vibrationEnabled: Boolean = true,
    val darkTheme: Boolean? = null // null = follow system
)

/**
 * Last successful connection saved for quick reconnect.
 */
@Serializable
data class ConnectionHistory(
    val ipAddress: String,
    val port: Int,
    val lastConnectedAt: Long = System.currentTimeMillis()
)

/**
 * Aggregated UI state for the controller screen.
 */
data class ControllerUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val latencyMs: Long = -1L,
    val settings: ControllerSettings = ControllerSettings(),
    val errorMessage: String? = null
)

/**
 * UI state for the connect screen.
 */
data class ConnectUiState(
    val ipAddress: String = "",
    val port: String = "27015",
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val recentConnections: List<ConnectionHistory> = emptyList(),
    val errorMessage: String? = null,
    val isConnecting: Boolean = false
)
