package com.pixel.mediachip.ui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixel.mediachip.R
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.components.ChipPreset
import com.pixel.mediachip.ui.components.PresetRow
import com.pixel.mediachip.ui.theme.HeroCardShape
import com.pixel.mediachip.ui.theme.PillShape
import com.pixel.mediachip.ui.theme.heroCardBackground
import com.pixel.mediachip.ui.theme.tactilePress

/**
 * M3 Expressive Hero Card hosting the real-time status bar simulation canvas,
 * squish simulation toggle, and 1-click preset switching pills.
 *
 * Specifications:
 * - 28dp Extra-Large corner radius ([HeroCardShape])
 * - surfaceContainer background elevation ([heroCardBackground])
 * - 16dp outer content padding with concentric inner geometry
 * - Zero hardcoded colors and zero emojis
 */
@Composable
fun LivePreviewCard(
    settings: PreviewChipSettings,
    onPresetSelected: ((ChipPreset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialSquished: Boolean = false,
    externalSquished: Boolean? = null,
    onSquishToggled: ((Boolean) -> Unit)? = null,
    trackTitle: String = "Starboy",
    artistName: String = "The Weeknd",
    showPresets: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var internalSquished by remember { mutableStateOf(initialSquished) }
    val isSquished = externalSquished ?: internalSquished
    var selectedPreset by remember { mutableStateOf<ChipPreset?>(null) }

    Card(
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.heroCardBackground
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header Row: Card Title & Squish Simulation Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.preview_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Squish Toggle Pill
                Surface(
                    onClick = {
                        val newSquish = !isSquished
                        if (settings.hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        if (onSquishToggled != null) {
                            onSquishToggled(newSquish)
                        } else {
                            internalSquished = newSquish
                        }
                    },
                    shape = PillShape,
                    color = if (isSquished) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (isSquished) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = if (isSquished) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    modifier = Modifier.tactilePress(pressedScale = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSquished) Icons.Rounded.Check else Icons.Rounded.Compress,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isSquished) stringResource(R.string.preview_squished) else stringResource(R.string.preview_squish),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 44dp Google Pixel Status Bar Canvas
            StatusBarCanvas(
                settings = settings,
                isSquished = isSquished,
                trackTitle = trackTitle,
                artistName = artistName
            )

            // Optional 1-Click Fast Presets Bar (when not pinned)
            if (showPresets && onPresetSelected != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.preview_fast_presets),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    PresetRow(
                        selectedPreset = selectedPreset,
                        onPresetSelected = { preset ->
                            selectedPreset = preset
                            onPresetSelected(preset)
                        },
                        hapticEnabled = settings.hapticEnabled
                    )
                }
            }
        }
    }
}

/**
 * Overload for [LivePreviewCard] taking [ChipSettings] directly.
 */
@Composable
fun LivePreviewCard(
    chipSettings: ChipSettings,
    onPresetSelected: ((ChipPreset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialSquished: Boolean = false,
    externalSquished: Boolean? = null,
    onSquishToggled: ((Boolean) -> Unit)? = null,
    trackTitle: String = "Starboy",
    artistName: String = "The Weeknd",
    showPresets: Boolean = false
) {
    LivePreviewCard(
        settings = chipSettings.toPreviewSettings(),
        onPresetSelected = onPresetSelected,
        modifier = modifier,
        initialSquished = initialSquished,
        externalSquished = externalSquished,
        onSquishToggled = onSquishToggled,
        trackTitle = trackTitle,
        artistName = artistName,
        showPresets = showPresets
    )
}
