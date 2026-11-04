package com.pixel.mediachip.ui.preview

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.theme.expressiveEffectsSpring
import com.pixel.mediachip.ui.theme.expressiveSpatialSpring

/**
 * Immutable preview configuration reflecting user settings for the status bar chip.
 */
data class PreviewChipSettings(
    val layoutMode: Int = ChipSettings.MODE_TWO_LINES,
    val squishMode: Int = ChipSettings.SQUISH_TO_ICON,
    val showArtist: Boolean = true,
    val marqueeOneLine: Boolean = true,
    val monetTint: Boolean = true,
    val swipeGestures: Boolean = true,
    val hapticEnabled: Boolean = true,
    val eatCamera: Boolean = false,
    val visualizerCardEnabled: Boolean = true,
    val visualizerChipEnabled: Boolean = true,
    val visualizerSensitivity: Int = 100,
    val maxWidthDp: Int = 105,
    val maxTextWidthDp: Int = 105,
    val cameraEatDepthDp: Int = 16,
    val titleSizeSp: Float = 10.0f,
    val artistSizeSp: Float = 8.0f,
    val cornerRadiusDp: Int = 14
)

/**
 * Extension to map [ChipSettings] to immutable [PreviewChipSettings].
 */
fun ChipSettings.toPreviewSettings(): PreviewChipSettings = PreviewChipSettings(
    layoutMode = layoutMode,
    squishMode = squishMode,
    showArtist = showArtist,
    marqueeOneLine = marqueeOneLine,
    monetTint = monetTint,
    swipeGestures = swipeGestures,
    hapticEnabled = hapticEnabled,
    eatCamera = eatCamera,
    visualizerCardEnabled = visualizerCardEnabled,
    visualizerChipEnabled = visualizerChipEnabled,
    visualizerSensitivity = visualizerSensitivity,
    maxWidthDp = maxWidthDp,
    maxTextWidthDp = maxTextWidthDp,
    cameraEatDepthDp = cameraEatDepthDp,
    titleSizeSp = titleSizeSp,
    artistSizeSp = artistSizeSp,
    cornerRadiusDp = cornerRadiusDp
)

/**
 * Real-time media chip preview component.
 *
 * Responds to:
 * - layoutMode (Two Lines / One Line / Icon Only)
 * - Typography (titleSizeSp, artistSizeSp, showArtist)
 * - Geometry (cornerRadiusDp, maxWidthDp, maxTextWidthDp)
 * - Material You Monet tint vs neutral container
 * - eatCamera lens depth expansion with safe text padding (cameraEatDepthDp + 6dp)
 * - Squish simulation (soft spring compression to 28dp pill or fade out)
 */
@Composable
fun SimulatedStatusChip(
    settings: PreviewChipSettings,
    modifier: Modifier = Modifier,
    isSquished: Boolean = false,
    trackTitle: String = "Starboy",
    artistName: String = "The Weeknd",
    isPlaying: Boolean = true
) {
    val isIconOnly = LivePreviewMath.isChipInIconOnlyState(
        layoutMode = settings.layoutMode,
        isSquished = isSquished,
        squishMode = settings.squishMode
    )

    val targetWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
        layoutMode = settings.layoutMode,
        maxWidthDp = settings.maxWidthDp,
        maxTextWidthDp = settings.maxTextWidthDp,
        eatCamera = settings.eatCamera,
        cameraEatDepthDp = settings.cameraEatDepthDp,
        isSquished = isSquished,
        squishMode = settings.squishMode
    )

    val isVisible = LivePreviewMath.isChipVisibleWhenSquished(
        isSquished = isSquished,
        squishMode = settings.squishMode
    )

    val targetAlpha = if (isVisible) 1.0f else 0.0f

    val targetCornerRadius = if (isIconOnly) {
        (LivePreviewMath.ICON_ONLY_SIZE_DP / 2).dp
    } else {
        settings.cornerRadiusDp.coerceIn(4, 24).dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth.dp,
        animationSpec = expressiveSpatialSpring(),
        label = "chipWidth"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = expressiveEffectsSpring(),
        label = "chipAlpha"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = expressiveSpatialSpring(),
        label = "chipCornerRadius"
    )

    val chipBackground = if (settings.monetTint) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val chipContentColor = if (settings.monetTint) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val safeEndPadding = if (isIconOnly) {
        0.dp
    } else {
        LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = settings.eatCamera,
            cameraEatDepthDp = settings.cameraEatDepthDp,
            basePaddingDp = LivePreviewMath.DEFAULT_BASE_PADDING_DP
        ).dp
    }

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(LivePreviewMath.ICON_ONLY_SIZE_DP.dp)
            .graphicsLayer {
                alpha = animatedAlpha
            }
            .clip(RoundedCornerShape(animatedCornerRadius))
            .background(chipBackground),
        contentAlignment = if (isIconOnly) Alignment.Center else Alignment.CenterStart
    ) {
        if (isIconOnly) {
            // Icon-only mode: display centered audio visualizer or music icon
            if (settings.visualizerChipEnabled) {
                MiniVisualizerBars(
                    isPlaying = isPlaying,
                    sensitivity = settings.visualizerSensitivity,
                    tint = chipContentColor,
                    barWidth = 2.dp,
                    barSpacing = 1.5.dp,
                    maxHeight = 12.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = chipContentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            // Expanded row with album/app icon, visualizer, and typography block
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 6.dp, end = safeEndPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Either audio visualizer waves OR music/album icon (mutually exclusive)
                if (settings.visualizerChipEnabled) {
                    MiniVisualizerBars(
                        isPlaying = isPlaying,
                        sensitivity = settings.visualizerSensitivity,
                        tint = chipContentColor,
                        barWidth = 1.8.dp,
                        barSpacing = 1.2.dp,
                        maxHeight = 10.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(chipContentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = chipContentColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                // 3. Typography block (Track title + Artist name)
                when (settings.layoutMode) {
                    ChipSettings.MODE_TWO_LINES -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = trackTitle,
                                fontSize = settings.titleSizeSp.sp,
                                lineHeight = (settings.titleSizeSp + 1f).sp,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = settings.titleSizeSp.sp,
                                    lineHeight = (settings.titleSizeSp + 1f).sp
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = chipContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (settings.showArtist) {
                                Text(
                                    text = artistName,
                                    fontSize = settings.artistSizeSp.sp,
                                    lineHeight = (settings.artistSizeSp + 1f).sp,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = settings.artistSizeSp.sp,
                                        lineHeight = (settings.artistSizeSp + 1f).sp
                                    ),
                                    fontWeight = FontWeight.Normal,
                                    color = chipContentColor.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    ChipSettings.MODE_ONE_LINE -> {
                        val displayText = if (settings.showArtist) {
                            "$trackTitle • $artistName"
                        } else {
                            trackTitle
                        }
                        Text(
                            text = displayText,
                            fontSize = settings.titleSizeSp.sp,
                            lineHeight = (settings.titleSizeSp + 2f).sp,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = settings.titleSizeSp.sp,
                                lineHeight = (settings.titleSizeSp + 2f).sp
                            ),
                            fontWeight = FontWeight.Medium,
                            color = chipContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overload for [SimulatedStatusChip] taking [ChipSettings] directly.
 */
@Composable
fun SimulatedStatusChip(
    chipSettings: ChipSettings,
    modifier: Modifier = Modifier,
    isSquished: Boolean = false,
    trackTitle: String = "Starboy",
    artistName: String = "The Weeknd",
    isPlaying: Boolean = true
) {
    SimulatedStatusChip(
        settings = chipSettings.toPreviewSettings(),
        modifier = modifier,
        isSquished = isSquished,
        trackTitle = trackTitle,
        artistName = artistName,
        isPlaying = isPlaying
    )
}
