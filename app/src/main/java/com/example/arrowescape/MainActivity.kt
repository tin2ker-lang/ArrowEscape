package com.example.arrowescape

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.example.arrowescape.ui.App
import com.example.arrowescape.ui.DarkPalette
import com.example.arrowescape.ui.LightPalette
import com.example.arrowescape.ui.LocalPalette
import com.example.arrowescape.ui.LocalThemeState
import com.example.arrowescape.ui.ThemeState

class MainActivity : ComponentActivity() {

    private lateinit var audio: GameAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("arrow_escape", Context.MODE_PRIVATE)
        audio = GameAudio(this, prefs)
        val theme = ThemeState(prefs)

        setContent {
            val dark = theme.isDark()

            // Status/navigation bar icons: light icons on dark mode, dark icons on light mode.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            val palette = if (dark) DarkPalette else LightPalette
            CompositionLocalProvider(
                LocalPalette provides palette,
                LocalThemeState provides theme,
            ) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(palette.background)
                            .systemBarsPadding()
                    ) {
                        App(prefs, audio)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        audio.onForeground()
    }

    override fun onPause() {
        super.onPause()
        audio.onBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        audio.release()
    }
}
