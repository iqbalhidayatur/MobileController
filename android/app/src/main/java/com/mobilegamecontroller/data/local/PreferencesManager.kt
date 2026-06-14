package com.mobilegamecontroller.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mobilegamecontroller.domain.model.ConnectionHistory
import com.mobilegamecontroller.domain.model.ControllerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "controller_prefs")

/**
 * Persists user settings and connection history using DataStore.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ANALOG_SENSITIVITY = floatPreferencesKey("analog_sensitivity")
        val ANALOG_DEADZONE = floatPreferencesKey("analog_deadzone")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DARK_THEME = stringPreferencesKey("dark_theme") // "true", "false", "system"
        val CONNECTION_HISTORY = stringPreferencesKey("connection_history")
    }

    val settings: Flow<ControllerSettings> = context.dataStore.data.map { prefs ->
        val themeValue = prefs[Keys.DARK_THEME] ?: "system"
        ControllerSettings(
            analogSensitivity = prefs[Keys.ANALOG_SENSITIVITY] ?: 1.0f,
            analogDeadzone = prefs[Keys.ANALOG_DEADZONE] ?: 0.12f,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            darkTheme = when (themeValue) {
                "true" -> true
                "false" -> false
                else -> null
            }
        )
    }

    val connectionHistory: Flow<List<ConnectionHistory>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.CONNECTION_HISTORY] ?: return@map emptyList()
        try {
            Json.decodeFromString<List<ConnectionHistory>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun updateSettings(settings: ControllerSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ANALOG_SENSITIVITY] = settings.analogSensitivity
            prefs[Keys.ANALOG_DEADZONE] = settings.analogDeadzone
            prefs[Keys.VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[Keys.DARK_THEME] = when (settings.darkTheme) {
                true -> "true"
                false -> "false"
                null -> "system"
            }
        }
    }

    suspend fun addConnectionHistory(history: ConnectionHistory) {
        context.dataStore.edit { prefs ->
            val current = try {
                val json = prefs[Keys.CONNECTION_HISTORY]
                if (json != null) Json.decodeFromString<List<ConnectionHistory>>(json) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val updated = (listOf(history) + current.filter {
                it.ipAddress != history.ipAddress || it.port != history.port
            }).take(5)
            prefs[Keys.CONNECTION_HISTORY] = Json.encodeToString(updated)
        }
    }
}
