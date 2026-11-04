package com.pixel.mediachip.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.theme.previewStatusBarBackground

/**
 * Authentic Google Pixel 44dp Status Bar Simulation Canvas.
 *
 * Renders:
 * - 44dp height AMOLED black bar container with 16dp top corner radius (phone screen curvature)
 * - Clock "09:41" on the left
 * - Reactive [SimulatedStatusChip] expanding towards center
 * - Center-aligned [CameraCutoutView] punch-hole camera lens
 * - Pixel system telemetry on the right (Wi-Fi, Cellular, Battery pill)
 * - Zero hardcoded colors and zero emojis
 */
@Composable
fun StatusBarCanvas(
    settings: PreviewChipSettings,
    modifier: Modifier = Modifier,
    isSquished: Boolean = false,
    trackTitle: String = "Starboy",
    artistName: String = "The Weeknd"
) {
    val barShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(barShape)
            .background(MaterialTheme.colorScheme.previewStatusBarBackground)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Left side: Clock & Media Chip
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "09:41",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = contentColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            SimulatedStatusChip(
                settings = settings,
                isSquished = isSquished,
                trackTitle = trackTitle,
                artistName = artistName
            )
        }

        // Center: Google Pixel Punch-Hole Camera Cutout
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CameraCutoutView()
        }

        // Right side: Network & Battery status telemetry
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )

            Icon(
                imageVector = Icons.Rounded.SignalCellular4Bar,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )

            PixelBatteryPill(
                chargeFraction = 0.82f,
                tint = contentColor
            )
        }
    }
}

/**
 * Pixel-style status bar battery indicator with terminal tip and fill level.
 */
@Composable
private fun PixelBatteryPill(
    chargeFraction: Float,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery Body
        Box(
            modifier = Modifier
                .size(width = 19.dp, height = 10.dp)
                .border(
                    width = 1.dp,
                    color = tint.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(2.5.dp)
                )
                .padding(1.5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(chargeFraction.coerceIn(0.1f, 1.0f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(tint)
            )
        }

        // Positive Terminal Nipple
        Box(
            modifier = Modifier
                .size(width = 1.5.dp, height = 4.dp)
                .clip(RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
                .background(tint.copy(alpha = 0.85f))
        )
    }
}

/**
 * Overload for [StatusBarCanvas] taking [ChipSettings] directly.
 */
@Composable
fun StatusBarCanvas(
    chipSettings: ChipSettings,
    modifier: Modifier = Modifier,
    isSquished: Boolean = false,
    trackTitle: String = "Starboy",
    artistName: String = "The Weeknd"
) {
    StatusBarCanvas(
        settings = chipSettings.toPreviewSettings(),
        modifier = modifier,
        isSquished = isSquished,
        trackTitle = trackTitle,
        artistName = artistName
    )
}
