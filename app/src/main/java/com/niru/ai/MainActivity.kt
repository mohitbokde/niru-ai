package com.niru.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        // App khulne par VOICE MODE hamesha OFF rahega, user khud ON karega
        prefs.voiceMode = false

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Color(0xFF6A4CC2))
            ) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F3FF)) {
                    NiruScreen(prefs)
                }
            }
        }
    }
}
