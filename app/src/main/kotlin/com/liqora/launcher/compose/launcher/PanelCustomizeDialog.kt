package com.liqora.launcher.compose.launcher

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min

/**
 * Size presets for the "Panel Size" picker. Values are grid-cell spans, clamped
 * to whatever room is actually available on the grid from the panel's current
 * top-left position so it never gets pushed off-grid.
 */
private data class PanelSizePreset(val label: String, val spanX: Int, val spanY: Int)

private val panelSizePresets = listOf(
    PanelSizePreset("Small", 1, 1),
    PanelSizePreset("Medium", 2, 2),
    PanelSizePreset("Large", 3, 3)
)

/**
 * Premium Blue Liquid Glass bottom-sheet-style dialog for customizing a single
 * GlassPanel from Edit Mode: size, style variant (per panelType), tint, opacity,
 * blur and corner radius. All edits apply live via [onUpdate] — there is no
 * separate "Save" step, matching how every other setting in this app behaves.
 */
@Composable
fun PanelCustomizeDialog(
    item: LauncherItem.GlassPanel,
    gridColumns: Int,
    gridRows: Int,
    onDismiss: () -> Unit,
    onUpdate: (LauncherItem.GlassPanel) -> Unit
) {
    val view = LocalView.current
    val colors = GlassThemeState.colors

    val tintPresets = listOf(
        0xFF0A84FFL, // Apple Blue (default)
        0xFF6366F1L, // Indigo
        0xFF30D158L, // Green
        0xFFFF9F0AL, // Orange
        0xFFEC4899L, // Pink
        0xFF64D2FFL, // Cyan
        0xFFBF5AF2L, // Purple
        0xFFFF453AL  // Red
    )

    val styleOptions = remember(item.panelType) { PanelStyleOptions.forType(item.panelType) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        colors.backgroundElevated,
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .border(
                        1.dp,
                        colors.glassBorder,
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
            ) {
                // Drag handle + header
                Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .background(colors.textSecondary.copy(alpha = 0.35f), CircleShape)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LiquidGlassSpacing.md, vertical = LiquidGlassSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Customize Panel",
                        style = LiquidGlassTypography.title3,
                        color = colors.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorder, CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, "Done", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.divider)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = LiquidGlassSpacing.md, vertical = LiquidGlassSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.lg)
                ) {
                    // --- Size ---
                    item {
                        CustomizeSection(title = "Panel Size") {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                panelSizePresets.forEach { preset ->
                                    val maxSpanX = max(1, gridColumns - item.gridX)
                                    val maxSpanY = max(1, gridRows - item.gridY)
                                    val clampedX = min(preset.spanX, maxSpanX)
                                    val clampedY = min(preset.spanY, maxSpanY)
                                    val isSelected = item.spanX == clampedX && item.spanY == clampedY
                                    CustomizeChip(
                                        label = preset.label,
                                        selected = isSelected,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onUpdate(item.copy(spanX = clampedX, spanY = clampedY))
                                    }
                                }
                            }
                            Text(
                                "${item.spanX} × ${item.spanY} grid cells",
                                style = LiquidGlassTypography.footnote,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // --- Style (only for panel types with variants) ---
                    if (styleOptions.isNotEmpty()) {
                        item {
                            CustomizeSection(title = "Panel Style") {
                                val effectiveStyle = item.panelStyle.ifBlank { styleOptions.first() }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    styleOptions.forEach { styleName ->
                                        CustomizeChip(
                                            label = styleName,
                                            selected = effectiveStyle == styleName,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            onUpdate(item.copy(panelStyle = styleName))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Tint ---
                    item {
                        CustomizeSection(title = "Background Tint") {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                tintPresets.forEach { presetColor ->
                                    val swatch = Color(presetColor.toInt())
                                    val isSelected = item.tintColor == presetColor
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(swatch)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onUpdate(item.copy(tintColor = swatch.toArgb().toLong()))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Opacity ---
                    item {
                        CustomizeSection(title = "Background Opacity") {
                            CustomizeSlider(
                                value = item.backgroundAlpha,
                                valueRange = 0.02f..0.6f,
                                valueLabel = "${(item.backgroundAlpha * 100).toInt()}%",
                                onValueChange = { onUpdate(item.copy(backgroundAlpha = it)) }
                            )
                        }
                    }

                    // --- Blur ---
                    item {
                        CustomizeSection(title = "Blur Intensity") {
                            CustomizeSlider(
                                value = item.blurRadius,
                                valueRange = 0f..50f,
                                valueLabel = "${item.blurRadius.toInt()}dp",
                                onValueChange = { onUpdate(item.copy(blurRadius = it)) }
                            )
                        }
                    }

                    // --- Corner Radius ---
                    item {
                        CustomizeSection(title = "Corner Radius") {
                            val usingGlobal = item.cornerRadius < 0f
                            val effectiveRadius = if (usingGlobal) 20f else item.cornerRadius
                            CustomizeSlider(
                                value = effectiveRadius,
                                valueRange = 0f..40f,
                                valueLabel = if (usingGlobal) "Auto" else "${effectiveRadius.toInt()}dp",
                                onValueChange = { onUpdate(item.copy(cornerRadius = it)) }
                            )
                            if (!usingGlobal) {
                                Text(
                                    "Reset to global",
                                    style = LiquidGlassTypography.footnote,
                                    color = colors.primary,
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            onUpdate(item.copy(cornerRadius = -1f))
                                        }
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(LiquidGlassSpacing.xl)) }
                }
            }
        }
    }
}

@Composable
private fun CustomizeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = GlassThemeState.colors
    Column {
        Text(
            title.uppercase(),
            style = LiquidGlassTypography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}

@Composable
private fun CustomizeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = GlassThemeState.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primary else colors.glassSurface)
            .border(
                1.dp,
                if (selected) Color.Transparent else colors.glassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CustomizeSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    val colors = GlassThemeState.colors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.glassSurface)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(valueLabel, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.glassBorder
            )
        )
    }
}
