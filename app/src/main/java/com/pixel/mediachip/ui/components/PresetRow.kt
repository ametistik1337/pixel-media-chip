package com.pixel.mediachip.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixel.mediachip.R
import com.pixel.mediachip.ui.theme.PillShape
import com.pixel.mediachip.ui.theme.tactilePress

enum class ChipPreset(val title: String, val titleRes: Int) {
    PIXEL_DEFAULT("По умолчанию", R.string.preset_default),
    DYNAMIC_ISLAND("Остров", R.string.preset_island),
    COMPACT("Компактный", R.string.preset_compact),
    EXPANDED("Расширенный", R.string.preset_expanded)
}

/**
 * Fast preset selection pills bar for instant layout and geometry switching.
 * Includes Emil Kowalski tactile press (0.95 scale) and discrete haptics.
 * Zero hardcoded colors, zero emojis.
 */
@Composable
fun PresetRow(
    selectedPreset: ChipPreset?,
    onPresetSelected: (ChipPreset) -> Unit,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipPreset.values().forEach { preset ->
            val isSelected = selectedPreset == preset

            Surface(
                onClick = {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onPresetSelected(preset)
                },
                shape = PillShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                modifier = Modifier.tactilePress(pressedScale = 0.95f)
            ) {
                Text(
                    text = stringResource(preset.titleRes),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
