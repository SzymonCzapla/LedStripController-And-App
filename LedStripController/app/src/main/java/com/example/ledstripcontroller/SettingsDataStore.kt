package com.example.ledstripcontroller

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("led_settings")

object SettingsKeys {
    val BRIGHTNESS = intPreferencesKey("brightness")
    val RED = intPreferencesKey("red")
    val GREEN = intPreferencesKey("green")
    val BLUE = intPreferencesKey("blue")
    val MODE = stringPreferencesKey("mode")
}

data class LedSettings(
    val brightness: Int = 255,
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val mode: String = "off"
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<LedSettings> = context.dataStore.data.map {
        LedSettings(
            brightness = it[SettingsKeys.BRIGHTNESS] ?: 255,
            red = it[SettingsKeys.RED] ?: 0,
            green = it[SettingsKeys.GREEN] ?: 0,
            blue = it[SettingsKeys.BLUE] ?: 0,
            mode = it[SettingsKeys.MODE] ?: "off"
        )
    }

    suspend fun saveSettings(
        brightness: Int? = null,
        red: Int? = null,
        green: Int? = null,
        blue: Int? = null,
        mode: String? = null
    ) {
        context.dataStore.edit {
            brightness?.let { b -> it[SettingsKeys.BRIGHTNESS] = b }
            red?.let { r -> it[SettingsKeys.RED] = r }
            green?.let { g -> it[SettingsKeys.GREEN] = g }
            blue?.let { b -> it[SettingsKeys.BLUE] = b }
            mode?.let { m -> it[SettingsKeys.MODE] = m }
        }
    }
}