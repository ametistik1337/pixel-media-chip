package com.pixel.mediachip.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Calculates the concentric corner radius for a nested child element.
 *
 * Geometric rule: In order to maintain visual harmony and a single center of
 * curvature, the inner radius must equal the outer radius minus the intervening padding:
 * R_inner = max(0, R_outer - padding)
 */
fun calculateConcentricRadius(outerRadius: Dp, padding: Dp): Dp {
    return maxOf(0.dp, outerRadius - padding)
}

/**
 * Returns a [RoundedCornerShape] with concentric radius calculated based on outerRadius and padding.
 */
fun concentricShape(outerRadius: Dp, padding: Dp): RoundedCornerShape {
    return RoundedCornerShape(calculateConcentricRadius(outerRadius, padding))
}

// Material 3 Expressive corner scale tokens
val ExpressiveCornerNone: Dp = 0.dp
val ExpressiveCornerExtraSmall: Dp = 4.dp
val ExpressiveCornerSmall: Dp = 8.dp
val ExpressiveCornerMedium: Dp = 12.dp
val ExpressiveCornerLarge: Dp = 16.dp
val ExpressiveCornerLargeIncreased: Dp = 20.dp
val ExpressiveCornerExtraLarge: Dp = 28.dp
val ExpressiveCornerExtraLargeIncreased: Dp = 32.dp
val ExpressiveCornerFull: Dp = 9999.dp

/**
 * M3 Expressive shape hierarchy.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(ExpressiveCornerExtraSmall),
    small = RoundedCornerShape(ExpressiveCornerSmall),
    medium = RoundedCornerShape(ExpressiveCornerMedium),
    large = RoundedCornerShape(ExpressiveCornerLarge),
    extraLarge = RoundedCornerShape(ExpressiveCornerExtraLarge)
)

val HeroCardShape: RoundedCornerShape = RoundedCornerShape(ExpressiveCornerExtraLarge)
val SettingsCardShape: RoundedCornerShape = RoundedCornerShape(ExpressiveCornerLarge)
val PillShape: RoundedCornerShape = CircleShape
