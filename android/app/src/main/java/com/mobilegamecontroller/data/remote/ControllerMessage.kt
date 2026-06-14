package com.mobilegamecontroller.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON wire format for controller events sent over TCP.
 * Each message is a single JSON object terminated by newline (\n).
 */
@Serializable
sealed class ControllerMessage {

    @Serializable
    @SerialName("button")
    data class Button(
        val type: String = "button",
        val button: String,
        val pressed: Boolean
    ) : ControllerMessage()

    @Serializable
    @SerialName("analog")
    data class Analog(
        val type: String = "analog",
        val stick: String,
        val x: Float,
        val y: Float
    ) : ControllerMessage()

    @Serializable
    @SerialName("ping")
    data class Ping(
        val type: String = "ping",
        val timestamp: Long = System.currentTimeMillis()
    ) : ControllerMessage()
}

/** Compact JSON encoder — no pretty print to minimize payload size. */
object ControllerJson {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encodeButton(button: String, pressed: Boolean): String =
        json.encodeToString(ControllerMessage.Button(button = button, pressed = pressed)) + "\n"

    fun encodeAnalog(stick: String, x: Float, y: Float): String =
        json.encodeToString(ControllerMessage.Analog(stick = stick, x = x, y = y)) + "\n"

    fun encodePing(): String =
        json.encodeToString(ControllerMessage.Ping()) + "\n"
}
