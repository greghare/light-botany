package com.thelightphone.botany

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object BotanyPreferences {
    val DEFAULT_CAMERA_FRONT = booleanPreferencesKey("default_camera_front")
    val LOCATION = stringPreferencesKey("location")
}

data class BotanySettings(
    val defaultCameraFront: Boolean = false,
    val location: String = "",
)
