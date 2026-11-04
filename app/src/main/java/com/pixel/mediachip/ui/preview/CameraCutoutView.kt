package com.pixel.mediachip.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixel.mediachip.ChipSettings

/**
 * Mathematical calculations and safe margin utilities for the camera punch-hole cutout
 * and media chip expansion geometry.
 */
object LivePreviewMath {
    const val DEFAULT_BASE_PADDING_DP = 6
    const val ICON_ONLY_SIZE_DP = 28
    const val CAMERA_PUNCH_HOLE_DIAMETER_DP = 11

    /**
     * Calculates the right-hand safe text margin inside the media chip.
     * When [eatCamera] is true, the chip expands under the front camera lens,
     * requiring the text content to be padded by [cameraEatDepthDp] + [basePaddingDp]
     * so that the title and artist text remain completely clear of the physical lens.
     */
    fun calculateCameraSafeTextPadding(
        eatCamera: Boolean,
        cameraEatDepthDp: Int,
        basePaddingDp: Int = DEFAULT_BASE_PADDING_DP
    ): Int {
        val safeDepth = maxOf(0, cameraEatDepthDp)
        val safeBase = maxOf(0, basePaddingDp)
        return if (eatCamera) safeDepth + safeBase else safeBase
    }

    /**
     * Calculates the effective target width in dp for the simulated media chip
     * based on layout mode, squish status, and eatCamera expansion.
     */
    fun calculateEffectiveChipWidthDp(
        layoutMode: Int,
        maxWidthDp: Int,
        maxTextWidthDp: Int,
        eatCamera: Boolean,
        cameraEatDepthDp: Int,
        isSquished: Boolean,
        squishMode: Int,
        iconOnlySizeDp: Int = ICON_ONLY_SIZE_DP
    ): Int {
        if (isSquished) {
            return when (squishMode) {
                ChipSettings.SQUISH_TO_ICON -> iconOnlySizeDp
                ChipSettings.SQUISH_HIDE -> 0
                ChipSettings.SQUISH_DISABLED -> calculateNormalWidth(
                    layoutMode = layoutMode,
                    maxWidthDp = maxWidthDp,
                    maxTextWidthDp = maxTextWidthDp,
                    eatCamera = eatCamera,
                    cameraEatDepthDp = cameraEatDepthDp,
                    iconOnlySizeDp = iconOnlySizeDp
                )
                else -> iconOnlySizeDp
            }
        }

        return calculateNormalWidth(
            layoutMode = layoutMode,
            maxWidthDp = maxWidthDp,
            maxTextWidthDp = maxTextWidthDp,
            eatCamera = eatCamera,
            cameraEatDepthDp = cameraEatDepthDp,
            iconOnlySizeDp = iconOnlySizeDp
        )
    }

    private fun calculateNormalWidth(
        layoutMode: Int,
        maxWidthDp: Int,
        maxTextWidthDp: Int,
        eatCamera: Boolean,
        cameraEatDepthDp: Int,
        iconOnlySizeDp: Int
    ): Int {
        val extraEat = if (eatCamera) maxOf(0, cameraEatDepthDp) else 0
        return when (layoutMode) {
            ChipSettings.MODE_ICON_ONLY -> iconOnlySizeDp
            ChipSettings.MODE_ONE_LINE -> maxOf(iconOnlySizeDp, maxTextWidthDp + extraEat)
            ChipSettings.MODE_TWO_LINES -> maxOf(iconOnlySizeDp, maxWidthDp + extraEat)
            else -> maxOf(iconOnlySizeDp, maxWidthDp + extraEat)
        }
    }

    /**
     * Determines whether the media chip should be visible given squish state and mode.
     */
    fun isChipVisibleWhenSquished(isSquished: Boolean, squishMode: Int): Boolean {
        return !(isSquished && squishMode == ChipSettings.SQUISH_HIDE)
    }

    /**
     * Determines whether the chip is currently collapsed to icon-only state
     * (either explicitly configured or compressed via squish).
     */
    fun isChipInIconOnlyState(layoutMode: Int, isSquished: Boolean, squishMode: Int): Boolean {
        if (layoutMode == ChipSettings.MODE_ICON_ONLY) return true
        if (isSquished && squishMode == ChipSettings.SQUISH_TO_ICON) return true
        return false
    }
}

/**
 * Authentic Pixel punch-hole camera simulation view.
 *
 * Renders an 11dp circular dark cutout with a subtle lens reflection glint
 * and a fine optical ring border. Zero hardcoded colors.
 */
@Composable
fun CameraCutoutView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = LivePreviewMath.CAMERA_PUNCH_HOLE_DIAMETER_DP.dp
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.TopStart
    ) {
        // Subtle optical lens glint reflection in top-left quadrant
        Box(
            modifier = Modifier
                .size(sizeDp * 0.35f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )
    }
}
