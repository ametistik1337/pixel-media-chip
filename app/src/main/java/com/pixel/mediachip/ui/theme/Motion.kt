package com.pixel.mediachip.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * M3 Expressive Spring Physics Specifications.
 * Damping ratio 0.88-0.90 provides organic settling without aggressive overshoot.
 */
const val ExpressiveSpatialSpringDamping: Float = 0.88f
const val ExpressiveEffectsSpringDamping: Float = Spring.DampingRatioNoBouncy // 1.0f

fun <T> expressiveSpatialSpring(
    dampingRatio: Float = ExpressiveSpatialSpringDamping,
    stiffness: Float = Spring.StiffnessMediumLow,
    visibilityThreshold: T? = null
): SpringSpec<T> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold
)

fun <T> expressiveEffectsSpring(
    dampingRatio: Float = ExpressiveEffectsSpringDamping,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): SpringSpec<T> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold
)

fun <T> expressiveFastSpring(
    dampingRatio: Float = 0.90f,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): SpringSpec<T> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold
)

/**
 * Emil Kowalski tactile press feedback modifier.
 * Scales element down to [pressedScale] (0.97f by default) on press with soft spring return.
 */
fun Modifier.tactilePress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpatialSpringDamping,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tactilePressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Emil Kowalski tactile press feedback modifier with internal interaction source.
 */
fun Modifier.tactilePress(
    pressedScale: Float = 0.97f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    tactilePress(interactionSource = interactionSource, pressedScale = pressedScale)
}
