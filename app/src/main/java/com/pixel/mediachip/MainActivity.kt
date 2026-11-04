package com.pixel.mediachip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pixel.mediachip.ui.screens.SettingsScreen
import com.pixel.mediachip.ui.theme.PixelMediaChipTheme

/**
 * Single-Activity entry point for Pixel Media Chip settings and configuration.
 *
 * Hosts the Material 3 Expressive [SettingsScreen] within [PixelMediaChipTheme],
 * enabling dynamic Monet color roles and edge-to-edge window insets.
 *
 * Zero hardcoded colors, zero emojis.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelMediaChipTheme {
                SettingsScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val chipSettings = ChipSettings.get()
        chipSettings.load(this)
        chipSettings.sendBroadcast(this)
    }
}
