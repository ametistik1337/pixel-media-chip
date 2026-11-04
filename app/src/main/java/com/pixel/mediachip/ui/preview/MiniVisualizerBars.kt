package com.pixel.mediachip.ui.preview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated 3-bar mini audio equalizer visualizer.
 * Simulates real-time audio playback using phase-shifted harmonic animations.
 *
 * Adheres strictly to M3 Expressive motion and zero hardcoded colors.
 */
@Composable
fun MiniVisualizerBars(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    sensitivity: Int = 100,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    barWidth: Dp = 2.dp,
    barSpacing: Dp = 1.5.dp,
    minHeight: Dp = 3.dp,
    maxHeight: Dp = 12.dp
) {
    val sensitivityFactor = (sensitivity / 100f).coerceIn(0.5f, 2.0f)
    val transition = rememberInfiniteTransition(label = "visualizerTransition")

    // Bar 1: Medium frequency (480ms period)
    val bar1Fraction by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1Height"
    )

    // Bar 2: High frequency (360ms period - phase shifted)
    val bar2Fraction by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2Height"
    )

    // Bar 3: Low-medium frequency (540ms period - distinct rhythm)
    val bar3Fraction by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 540, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3Height"
    )

    val currentBar1 = if (isPlaying) {
        val amp = (maxHeight - minHeight) * bar1Fraction * sensitivityFactor
        (minHeight + amp).coerceIn(minHeight, maxHeight)
    } else {
        minHeight
    }

    val currentBar2 = if (isPlaying) {
        val amp = (maxHeight - minHeight) * bar2Fraction * sensitivityFactor
        (minHeight + amp).coerceIn(minHeight, maxHeight)
    } else {
        minHeight
    }

    val currentBar3 = if (isPlaying) {
        val amp = (maxHeight - minHeight) * bar3Fraction * sensitivityFactor
        (minHeight + amp).coerceIn(minHeight, maxHeight)
    } else {
        minHeight
    }

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VisualizerBar(height = currentBar1, width = barWidth, color = tint)
        VisualizerBar(height = currentBar2, width = barWidth, color = tint)
        VisualizerBar(height = currentBar3, width = barWidth, color = tint)
    }
}

@Composable
private fun VisualizerBar(
    height: Dp,
    width: Dp,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .background(color)
    )
}
