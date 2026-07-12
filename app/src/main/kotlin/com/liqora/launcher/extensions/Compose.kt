package com.liqora.launcher.extensions

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.liqora.launcher.compose.launcher.LiquidGlassElevation

/**
 * Extension function to easily set up Compose content in a View-based Activity
 * This allows gradual migration from View to Compose
 */
fun ComponentActivity.setComposeContent(content: @Composable () -> Unit) {
    setContent {
        content()
    }
}

/**
 * Extension function to create a ComposeView that can be added to existing ViewGroups
 * This allows mixing Compose and View components
 */
fun ViewGroup.addComposeView(content: @Composable () -> Unit): ComposeView {
    val composeView = ComposeView(context).apply {
        setContent {
            content()
        }
    }
    addView(composeView)
    return composeView
}

/**
 * Extension function to create a ComposeView without adding it
 * Useful when you need to add it at a specific position
 */
fun ViewGroup.createComposeView(content: @Composable () -> Unit): ComposeView {
    return ComposeView(context).apply {
        setContent {
            content()
        }
    }
}

/**
 * Soft blue "premium glow" used behind elevated glass surfaces (sheets,
 * FABs, active cards, dialogs) so they read as floating rather than flat.
 * Pairs a wide, low-alpha ambient [glowColor] shadow with a physical
 * [shadowElevation] so the effect grounds the surface instead of just
 * looking hazy. Uses [LiquidGlassElevation] presets for consistent depth
 * across every screen — pass [level] to match the surface's importance.
 */
fun Modifier.premiumGlow(
    glowColor: Color,
    shape: Shape = RoundedCornerShape(22.dp),
    level: LiquidGlassElevation.Level = LiquidGlassElevation.raised
): Modifier = this
    .shadow(
        elevation = level.glowRadius,
        shape = shape,
        ambientColor = glowColor.copy(alpha = level.glowAlpha),
        spotColor = glowColor.copy(alpha = level.glowAlpha)
    )
    .shadow(
        elevation = level.shadowElevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.18f),
        spotColor = Color.Black.copy(alpha = 0.24f)
    )
