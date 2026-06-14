package com.mobilegamecontroller.data.repository

import com.mobilegamecontroller.data.local.PreferencesManager
import com.mobilegamecontroller.data.remote.ControllerJson
import com.mobilegamecontroller.data.remote.TcpSocketClient
import com.mobilegamecontroller.domain.model.AnalogStick
import com.mobilegamecontroller.domain.model.ConnectionHistory
import com.mobilegamecontroller.domain.model.ConnectionState
import com.mobilegamecontroller.domain.model.ControllerButton
import com.mobilegamecontroller.domain.model.ControllerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Single source of truth bridging UI, preferences, and network layer.
 */
@Singleton
class ControllerRepository @Inject constructor(
    private val tcpClient: TcpSocketClient,
    private val preferencesManager: PreferencesManager
) {
    val connectionState: StateFlow<ConnectionState> = tcpClient.connectionState
    val latencyMs: StateFlow<Long> = tcpClient.latencyMs
    val settings: Flow<ControllerSettings> = preferencesManager.settings
    val connectionHistory: Flow<List<ConnectionHistory>> = preferencesManager.connectionHistory

    suspend fun connect(host: String, port: Int, scope: CoroutineScope): Result<Unit> {
        tcpClient.enableAutoReconnect()
        val result = tcpClient.connect(host, port, scope)
        if (result.isSuccess) {
            preferencesManager.addConnectionHistory(
                ConnectionHistory(ipAddress = host, port = port)
            )
        }
        return result
    }

    fun disconnect() {
        tcpClient.disconnect()
    }

    suspend fun sendButton(button: ControllerButton, pressed: Boolean): Result<Unit> {
        return tcpClient.send(
            ControllerJson.encodeButton(button.name, pressed),
            flushImmediately = true
        )
    }

    /**
     * Apply deadzone and sensitivity before sending analog values.
     * Values are normalized to -1.0 .. 1.0 range.
     */
    suspend fun sendAnalog(
        stick: AnalogStick,
        rawX: Float,
        rawY: Float,
        settings: ControllerSettings
    ): Result<Unit> {
        val (x, y) = applyDeadzoneAndSensitivity(rawX, rawY, settings)
        return tcpClient.send(
            ControllerJson.encodeAnalog(stick.name.lowercase(), x, y),
            // Analog updates are high-frequency; always flush for lowest latency
            flushImmediately = true
        )
    }

    suspend fun updateSettings(settings: ControllerSettings) {
        preferencesManager.updateSettings(settings)
    }

    /**
     * Deadzone: ignore small movements near center.
     * Sensitivity: scale output magnitude after deadzone removal.
     */
    private fun applyDeadzoneAndSensitivity(
        rawX: Float,
        rawY: Float,
        settings: ControllerSettings
    ): Pair<Float, Float> {
        val magnitude = hypot(rawX.toDouble(), rawY.toDouble()).toFloat()
        if (magnitude < settings.analogDeadzone) {
            return 0f to 0f
        }

        // Remap magnitude from [deadzone, 1] to [0, 1]
        val remapped = ((magnitude - settings.analogDeadzone) / (1f - settings.analogDeadzone))
            .coerceIn(0f, 1f) * settings.analogSensitivity

        val angle = atan2(rawY, rawX)
        val x = (cos(angle) * remapped).coerceIn(-1f, 1f)
        val y = (sin(angle) * remapped).coerceIn(-1f, 1f)
        return x to y
    }
}
