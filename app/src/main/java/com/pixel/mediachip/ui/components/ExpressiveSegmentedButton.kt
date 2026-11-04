package com.pixel.mediachip.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ViewHeadline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.pixel.mediachip.R
import com.pixel.mediachip.ChipSettings

/**
 * Expressive segmented button for switching display modes:
 * - Two Lines (MODE_TWO_LINES = 0)
 * - One Line (MODE_ONE_LINE = 1)
 * - Icon (MODE_ICON_ONLY = 2)
 *
 * Provides tactile haptic feedback on selection change. Zero hardcoded colors, zero emojis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSegmentedButton(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val modes = listOf(
        Triple(ChipSettings.MODE_TWO_LINES, stringResource(R.string.mode_two_lines), Icons.Rounded.ViewHeadline),
        Triple(ChipSettings.MODE_ONE_LINE, stringResource(R.string.mode_one_line), Icons.AutoMirrored.Rounded.ShortText),
        Triple(ChipSettings.MODE_ICON_ONLY, stringResource(R.string.mode_icon_only), Icons.Rounded.GraphicEq)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        modes.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = {
                    if (selectedMode != mode) {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onModeSelected(mode)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selectedMode == mode) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
