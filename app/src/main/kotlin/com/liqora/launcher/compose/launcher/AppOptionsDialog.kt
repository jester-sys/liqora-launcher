package com.liqora.launcher.compose.launcher

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun AppOptionsDialog(
    app: AvailableApp,
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    panelColor: Color,
    panelAlpha: Float,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onKill: () -> Unit,
    onUninstall: () -> Unit,
    onUsageAccess: () -> Unit
) {
    val colors = GlassThemeState.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(LiquidGlassRadius.shapeLg)
                .then(
                    if (glassSettings.drawerBlurEnabled) {
                        // Panel jaisa hi real backdrop blur — same backdrop source
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24f) },
                            effects = {
                                if (glassSettings.vibrancyEnabled) vibrancy()
                                blur(glassSettings.drawerBlurRadius.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(panelColor.copy(alpha = (panelAlpha + 0.20f).coerceIn(0f, 1f)))
                            }
                        )
                    } else {
                        Modifier.background(panelColor.copy(alpha = (panelAlpha + 0.20f).coerceIn(0f, 1f)))
                    }
                )
                .border(1.dp, colors.glassBorder, LiquidGlassRadius.shapeLg)
                .padding(LiquidGlassSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header
            val iconBitmap = remember(app.icon) { app.icon?.toBitmap(96, 96)?.asImageBitmap() }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.glassSurface),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                }
            }

            Spacer(Modifier.height(LiquidGlassSpacing.sm))

            Text(
                text = app.label,
                style = LiquidGlassTypography.callout,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = LiquidGlassTypography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(LiquidGlassSpacing.lg))

            OptionRow(
                icon = Icons.Rounded.Edit,
                label = "Edit App Info",
                tint = colors.primary,
                colors = colors,
                onClick = onEdit   // 👈 bas ye — onDismiss() hata de
            )
            OptionRow(
                icon = Icons.Rounded.Close,
                label = "Kill App",
                tint = Color(0xFFEF4444),
                colors = colors,
                onClick = onKill
            )
            OptionRow(
                icon = Icons.Rounded.Delete,
                label = "Uninstall",
                tint = Color(0xFFEF4444),
                colors = colors,
                onClick = onUninstall
            )
            OptionRow(
                icon = Icons.Rounded.History,
                label = "App Usage Access",
                tint = colors.primary,
                colors = colors,
                onClick = onUsageAccess
            )

            Spacer(Modifier.height(LiquidGlassSpacing.sm))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    }
}

/**
 * Single option row — icon chip on the left + label. Uses an explicit
 * rememberRipple() instead of LocalIndication.current: on some Compose
 * versions/themes LocalIndication.current can resolve to a provider that
 * silently swallows the click instead of firing onClick — rememberRipple()
 * avoids that entirely and is what actually fixes taps doing nothing.
 */
@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    colors: LiquidGlassColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint),
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = LiquidGlassSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(LiquidGlassSpacing.sm))
        Text(
            text = label,
            style = LiquidGlassTypography.body,
            color = colors.textPrimary
        )
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
fun EditAppInfoDialog(
    app: AvailableApp,
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    panelColor: Color,
    panelAlpha: Float,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit // label, iconUri
) {
    val colors = GlassThemeState.colors
    var label by remember { mutableStateOf(app.label) }
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                // Ignore if not supported
            }
            iconUri = uri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(LiquidGlassRadius.shapeLg)
                .then(
                    if (glassSettings.drawerBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24f) },
                            effects = {
                                if (glassSettings.vibrancyEnabled) vibrancy()
                                blur(glassSettings.drawerBlurRadius.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(panelColor.copy(alpha = (panelAlpha + 0.20f).coerceIn(0f, 1f)))
                            }
                        )
                    } else {
                        Modifier.background(panelColor.copy(alpha = (panelAlpha + 0.20f).coerceIn(0f, 1f)))
                    }
                )
                .border(1.dp, colors.glassBorder, LiquidGlassRadius.shapeLg)
                .padding(LiquidGlassSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Edit App Info",
                style = LiquidGlassTypography.subheadline,
                color = colors.textPrimary
            )

            Spacer(Modifier.height(LiquidGlassSpacing.lg))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (iconUri != null) {
                    AsyncImage(
                        model = iconUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val iconBitmap = remember(app.icon) { app.icon?.toBitmap(96, 96)?.asImageBitmap() }
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(LiquidGlassSpacing.lg))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("App Name", color = colors.textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.glassBorder,
                    cursorColor = colors.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(LiquidGlassSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(label, iconUri?.toString()) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}
