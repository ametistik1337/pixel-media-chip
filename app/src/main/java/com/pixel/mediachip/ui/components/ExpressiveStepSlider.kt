package com.pixel.mediachip.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pixel.mediachip.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Material 3 Expressive Slider (Official Google Material 3 specification):
 * - Continuous/discrete track with pill ends (m3.material.io/components/sliders/overview)
 * - Active track pill (outer corner = trackH/2, inner corner adjacent to thumb = 3.5dp)
 * - Inactive track pill (inner corner = 3.5dp, outer corner = trackH/2)
 * - Thumb indicator: primary vertical pill extending above and below the track with 4dp gaps
 * - Stop indicator dot at the far right end of inactive track
 * - Embedded reset button right beside the slider track for instantaneous default recovery
 * - Tactile press spring physics (0.98x, 0.9 damping, 700 stiffness)
 * - Crisp discrete step snap with anti-bounce haptics
 * - Zero hardcoded colors, zero emojis.
 */
@Composable
fun ExpressiveStepSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    label: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticEnabled: Boolean = true,
    defaultValue: Float? = null,
    onReset: (() -> Unit)? = null,
    trackHeight: Dp = 16.dp,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

    val initialStepIndex = ((value - valueRange.start) / step).roundToInt()
    var lastHapticStep by remember { mutableIntStateOf(initialStepIndex) }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        lastHapticStep = ((value - valueRange.start) / step).roundToInt()
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
        label = "expressive_slider_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Label & Formatted Value Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )

            val formattedValue = if (step < 1.0f) {
                String.format(Locale.US, "%.1f", value)
            } else {
                String.format(Locale.US, "%.0f", value)
            }
            val displayText = if (unit.isNotEmpty()) "$formattedValue $unit" else formattedValue

            Surface(
                shape = CircleShape,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider Track + Reset Button Row
        val activeColor = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        }
        val inactiveColor = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f)
        }
        val dotColor = if (enabled) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // M3 Expressive Slider Canvas with Touch Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .scale(pressScale)
                    .pointerInput(valueRange, step, enabled, hapticEnabled) {
                        if (!enabled) return@pointerInput

                        fun updateFromX(x: Float) {
                            val w = size.width.toFloat()
                            if (w <= 0f) return
                            val thumbW = 4.dp.toPx()
                            val available = (w - thumbW).coerceAtLeast(1f)
                            val rawFraction = ((x - thumbW / 2f) / available).coerceIn(0f, 1f)
                            val rawValue = valueRange.start + rawFraction * (valueRange.endInclusive - valueRange.start)
                            val snapped = ((rawValue - valueRange.start) / step).roundToInt() * step + valueRange.start
                            val clamped = snapped.coerceIn(valueRange.start, valueRange.endInclusive)

                            if (clamped != value) {
                                val newStep = ((clamped - valueRange.start) / step).roundToInt()
                                if (newStep != lastHapticStep && hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticStep = newStep
                                }
                                currentOnValueChange(clamped)
                            }
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            updateFromX(down.position.x)

                            val drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                                updateFromX(change.position.x)
                            }

                            if (drag != null) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    updateFromX(change.position.x)
                                }
                            }

                            isPressed = false
                            currentOnFinished?.invoke()
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val totalW = size.width
                    val totalH = size.height
                    if (totalW <= 0f || totalH <= 0f) return@Canvas

                    val trackH = trackHeight.toPx()
                    val trackTop = (totalH - trackH) / 2f
                    val trackOuterCorner = trackH / 2f
                    val innerCorner = 2.dp.toPx()

                    val thumbW = 4.dp.toPx()
                    val thumbH = 28.dp.toPx()
                    val thumbTop = (totalH - thumbH) / 2f
                    val thumbCorner = thumbW / 2f
                    val gap = 4.dp.toPx()

                    val rangeSpan = valueRange.endInclusive - valueRange.start
                    val fraction = if (rangeSpan > 0f) {
                        ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val availableW = totalW - thumbW
                    val thumbLeft = (fraction * availableW).coerceIn(0f, availableW)
                    val thumbRight = thumbLeft + thumbW

                    // 1. Active Track Pill (Left segment)
                    val activeRight = (thumbLeft - gap).coerceAtLeast(0f)
                    if (activeRight > 0f) {
                        val leftCorner = trackOuterCorner.coerceAtMost(activeRight / 2f)
                        val rightCorner = innerCorner.coerceAtMost(activeRight / 2f)
                        val activePath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(Offset(0f, trackTop), Size(activeRight, trackH)),
                                    topLeft = CornerRadius(leftCorner, leftCorner),
                                    bottomLeft = CornerRadius(leftCorner, leftCorner),
                                    topRight = CornerRadius(rightCorner, rightCorner),
                                    bottomRight = CornerRadius(rightCorner, rightCorner)
                                )
                            )
                        }
                        drawPath(activePath, activeColor)
                    }

                    // 2. Inactive Track Pill (Right segment)
                    val inactiveLeft = (thumbRight + gap).coerceAtMost(totalW)
                    val inactiveWidth = (totalW - inactiveLeft).coerceAtLeast(0f)
                    if (inactiveWidth > 0f) {
                        val leftCorner = innerCorner.coerceAtMost(inactiveWidth / 2f)
                        val rightCorner = trackOuterCorner.coerceAtMost(inactiveWidth / 2f)
                        val inactivePath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(Offset(inactiveLeft, trackTop), Size(inactiveWidth, trackH)),
                                    topLeft = CornerRadius(leftCorner, leftCorner),
                                    bottomLeft = CornerRadius(leftCorner, leftCorner),
                                    topRight = CornerRadius(rightCorner, rightCorner),
                                    bottomRight = CornerRadius(rightCorner, rightCorner)
                                )
                            )
                        }
                        drawPath(inactivePath, inactiveColor)
                    }

                    // 3. Stop Indicator Dot on far right of inactive track
                    val dotRadius = 2.dp.toPx()
                    val dotCenterX = totalW - (trackH / 2f)
                    val dotCenterY = totalH / 2f
                    if (dotCenterX > inactiveLeft + innerCorner + dotRadius && dotCenterX < totalW) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(dotCenterX, dotCenterY)
                        )
                    }

                    // 4. Primary Vertical Pill Thumb Indicator
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(thumbLeft, thumbTop),
                        size = Size(thumbW, thumbH),
                        cornerRadius = CornerRadius(thumbCorner, thumbCorner)
                    )
                }
            }

            // Reset Button next to each slider
            if (onReset != null && defaultValue != null) {
                val isModified = kotlin.math.abs(value - defaultValue) > 0.001f
                IconButton(
                    onClick = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onReset()
                    },
                    enabled = enabled && isModified,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (enabled && isModified) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
                                contentDescription = stringResource(R.string.slider_reset_default),
                                tint = if (enabled && isModified) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
