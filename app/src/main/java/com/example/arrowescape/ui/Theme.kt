package com.example.arrowescape.ui

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Light/dark choice. Follows the phone's setting until the player picks one. */
class ThemeState(private val prefs: SharedPreferences) {
    var mode by mutableStateOf(prefs.getString("theme", "system") ?: "system")
        private set

    fun toggle(currentlyDark: Boolean) {
        mode = if (currentlyDark) "light" else "dark"
        prefs.edit().putString("theme", mode).apply()
    }

    @Composable
    fun isDark(): Boolean = when (mode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalThemeState = staticCompositionLocalOf<ThemeState> { error("ThemeState not provided") }
