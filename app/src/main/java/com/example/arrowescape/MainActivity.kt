package com.example.arrowescape

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.arrowescape.ui.GameScreen
import com.example.arrowescape.ui.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("arrow_escape", Context.MODE_PRIVATE)
        setContent {
            MaterialTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Palette.background)
                        .systemBarsPadding()
                ) {
                    GameScreen(prefs)
                }
            }
        }
    }
}
