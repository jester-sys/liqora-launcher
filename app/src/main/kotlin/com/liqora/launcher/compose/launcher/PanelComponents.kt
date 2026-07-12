package com.liqora.launcher.compose.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.liqora.launcher.helpers.WeatherRepository
import com.liqora.launcher.helpers.WeatherStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlassPanelBackground(
    item: LauncherItem.GlassPanel,
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    isEditMode: Boolean
) {
    // Per-panel overrides: cornerRadius of -1 means "inherit the global setting".
    // backgroundAlpha/blurRadius are already per-panel fields on the model — use
    // them directly so the Customize Panel dialog has something real to control.
    val cornerRadius = (if (item.cornerRadius >= 0f) item.cornerRadius else glassSettings.panelCornerRadius).dp
    val customImageUri = item.customImageUri
    val panelTintColor = Color(item.tintColor.toInt())
    val effectiveBackgroundAlpha = item.backgroundAlpha
    val effectiveBlurRadius = item.blurRadius

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (item.customImageUri != null) {
                    Modifier
                } else if (glassSettings.liquidGlassEnabled) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(cornerRadius) },
                        effects = {
                            if (glassSettings.vibrancyEnabled) vibrancy()
                            // Corrected: Respect panelBlurEnabled. Uses this panel's own
                            // blurRadius so each panel can be customized independently.
                            if (glassSettings.blurEnabled && glassSettings.panelBlurEnabled) {
                                blur(effectiveBlurRadius.dp.toPx())
                            }
                            if (glassSettings.lensEnabled) lens(
                                refractionHeight = glassSettings.refractionHeight.dp.toPx(),
                                refractionAmount = glassSettings.refractionAmount.dp.toPx(),
                                chromaticAberration = glassSettings.chromaticAberration
                            )
                        },
                        onDrawSurface = {
                            drawRect(panelTintColor.copy(alpha = effectiveBackgroundAlpha))
                        }
                    )
                } else {
                    Modifier.background(
                        color = Color.Black.copy(alpha = effectiveBackgroundAlpha),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                }
            )
    ) {
        if (customImageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(customImageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f)
            )
        }
    }
}

@Composable
fun GlassPanelContent(
    item: LauncherItem.GlassPanel,
    glassSettings: LiquidGlassSettings,
    isEditMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (item.panelType == PanelType.SEARCH) 8.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (glassSettings.cyberpunkTheme) {
            when (item.panelType) {
                PanelType.CLOCK -> CyberpunkClock()
                PanelType.WEATHER -> CyberpunkWeatherPanelContent(glassSettings)
                PanelType.BATTERY -> CyberpunkBatteryPanelContent()
                else -> StandardPanelRouter(item, glassSettings, isEditMode)
            }
        } else {
            StandardPanelRouter(item, glassSettings, isEditMode)
        }
    }
}

@Composable
private fun StandardPanelRouter(
    item: LauncherItem.GlassPanel,
    glassSettings: LiquidGlassSettings,
    isEditMode: Boolean
) {
    // Per-panel style override: an empty panelStyle falls back to the matching
    // app-wide style in LiquidGlassSettings, so existing panels (saved before this
    // feature existed) keep behaving exactly as before.
    val clockStyle = item.panelStyle.ifBlank { glassSettings.clockStyle }
    val weatherStyle = item.panelStyle.ifBlank { glassSettings.weatherStyle }
    val batteryStyle = item.panelStyle.ifBlank { glassSettings.batteryStyle }

    when (item.panelType) {
        PanelType.CLOCK -> ClockPanelContent(glassSettings, styleOverride = clockStyle)
        PanelType.WEATHER -> WeatherPanelContent(glassSettings, styleOverride = weatherStyle)
        PanelType.BATTERY -> BatteryPanelContent(glassSettings, styleOverride = batteryStyle)
        PanelType.QUICK_SETTINGS -> QuickSettingsPanelContent()
        PanelType.MEDIA_CONTROL -> MediaControlPanelContent()
        PanelType.APPS -> AppGridPanelContent(item, glassSettings)
        PanelType.SEARCH -> BrowserSearchPanelContent(glassSettings.searchWidgetOpensBrowserOnTap, isEditMode)
        PanelType.PLAY_INTEGRITY -> PlayIntegrityPanelContent(glassSettings)
        else -> EmptyPanelPlaceholder(item, isEditMode)
    }
}

/**
 * Shown for a decorative EMPTY panel (or a CUSTOM panel with no image/title
 * set yet) instead of a bare rectangle. In edit mode it doubles as a gentle
 * nudge toward the Customize action; outside edit mode it stays quiet — a
 * soft glyph rather than an obviously "unfinished" blank box.
 */
@Composable
private fun EmptyPanelPlaceholder(item: LauncherItem.GlassPanel, isEditMode: Boolean) {
    if (item.title.isNotEmpty()) {
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        return
    }
    if (item.customImageUri != null) return // background layer already shows the image

    val colors = GlassThemeState.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.xxs)
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = colors.textTertiary.copy(alpha = if (isEditMode) 0.65f else 0.35f),
            modifier = Modifier.size(22.dp)
        )
        if (isEditMode) {
            Text(
                text = "Tap ✦ to customize",
                style = LiquidGlassTypography.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Style variants available per panel type — used to drive the Customize Panel style picker. */
object PanelStyleOptions {
    val clock = listOf("Classic", "Headline", "Vertical", "Minimal", "Analog")
    val weather = listOf("Classic", "Compact")
    val battery = listOf("Classic", "Minimal")

    fun forType(type: PanelType): List<String> = when (type) {
        PanelType.CLOCK -> clock
        PanelType.WEATHER -> weather
        PanelType.BATTERY -> battery
        else -> emptyList()
    }
}

@Composable
private fun CyberpunkWeatherPanelContent(glassSettings: LiquidGlassSettings) {
    val weatherData by WeatherStateRepository.weatherData.collectAsState()

    LaunchedEffect(glassSettings.openWeatherApiKey, glassSettings.weatherSource, glassSettings.weatherUnit) {
        if (glassSettings.weatherSource == "OpenWeather" && glassSettings.openWeatherApiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {
                WeatherRepository.fetchForecast(
                    lat = 0.0, lon = 0.0,
                    units = if (glassSettings.weatherUnit == "C") "metric" else "imperial",
                    apiKey = glassSettings.openWeatherApiKey
                )
            }
        }
    }
    val temp = weatherData?.currentTemp ?: "22°C"
    val desc = weatherData?.location ?: "Sunny"
    CyberpunkWeather(temp, Icons.Rounded.WbSunny, desc)
}

@Composable
private fun CyberpunkBatteryPanelContent() {
    val context = LocalContext.current
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
    val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
            status == android.os.BatteryManager.BATTERY_STATUS_FULL
    CyberpunkBattery(level, isCharging)
}

@Composable
fun ClockPanelContent(glassSettings: LiquidGlassSettings, styleOverride: String? = null) {
    val style = styleOverride ?: glassSettings.clockStyle
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val cal = Calendar.getInstance().apply { timeInMillis = currentTime }

    val hours = cal.get(Calendar.HOUR).toFloat() + cal.get(Calendar.MINUTE) / 60f
    val minutes = cal.get(Calendar.MINUTE).toFloat() + cal.get(Calendar.SECOND) / 60f
    val seconds = cal.get(Calendar.SECOND).toFloat()

    when (style) {
        "Headline" -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = dateFormat.format(Date(currentTime)).uppercase(),
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
        "Vertical" -> {
            val h = SimpleDateFormat("HH", Locale.getDefault()).format(Date(currentTime))
            val m = SimpleDateFormat("mm", Locale.getDefault()).format(Date(currentTime))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = h,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                )
                Text(
                    text = m,
                    color = GlassThemeState.colors.primary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 40.sp
                )
            }
        }
        "Minimal" -> {
            Text(
                text = timeFormat.format(Date(currentTime)),
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Thin
            )
        }
        "Analog" -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawArc(
                            Color.White.copy(0.06f),
                            -90f,
                            360f,
                            false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                        )
                        drawArc(
                            GlassThemeState.colors.primary,
                            -90f,
                            (minutes / 60f) * 360f,
                            false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        "Classic" -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.minDimension / 2f
                        drawCircle(Color.White.copy(0.06f), radius = radius)

                        for (i in 0 until 60) {
                            val angle = i * 6f - 90f
                            val rad = Math.toRadians(angle.toDouble()).toFloat()
                            val inner = if (i % 5 == 0) radius * 0.78f else radius * 0.86f
                            val outer = radius * 0.95f
                            drawLine(
                                color = Color.White.copy(if (i % 5 == 0) 0.9f else 0.25f),
                                start = Offset(cx + inner * kotlin.math.cos(rad), cy + inner * kotlin.math.sin(rad)),
                                end = Offset(cx + outer * kotlin.math.cos(rad), cy + outer * kotlin.math.sin(rad)),
                                strokeWidth = if (i % 5 == 0) 2f else 1f
                            )
                        }

                        val hAngle = (hours / 12f) * 360f - 90f
                        val hRad = Math.toRadians(hAngle.toDouble()).toFloat()
                        drawLine(
                            Color.White,
                            Offset(cx, cy),
                            Offset(cx + radius * 0.45f * kotlin.math.cos(hRad), cy + radius * 0.45f * kotlin.math.sin(hRad)),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )

                        val mAngle = (minutes / 60f) * 360f - 90f
                        val mRad = Math.toRadians(mAngle.toDouble()).toFloat()
                        drawLine(
                            Color.White,
                            Offset(cx, cy),
                            Offset(cx + radius * 0.65f * kotlin.math.cos(mRad), cy + radius * 0.65f * kotlin.math.sin(mRad)),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )

                        val sAngle = (seconds / 60f) * 360f - 90f
                        val sRad = Math.toRadians(sAngle.toDouble()).toFloat()
                        drawLine(
                            Color(0xFFFF6B6B),
                            Offset(cx, cy),
                            Offset(cx + radius * 0.78f * kotlin.math.cos(sRad), cy + radius * 0.78f * kotlin.math.sin(sRad)),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(Color.White, radius = 4f)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = dateFormat.format(Date(currentTime)),
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp
                )
            }
        }
        else -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = dateFormat.format(Date(currentTime)),
                    color = Color.White.copy(0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun WeatherPanelContent(glassSettings: LiquidGlassSettings, styleOverride: String? = null) {
    val style = styleOverride ?: glassSettings.weatherStyle
    val fetched by WeatherStateRepository.weatherData.collectAsState()
    val apiKey = glassSettings.openWeatherApiKey
    val units = if (glassSettings.weatherUnit == "C") "metric" else "imperial"

    LaunchedEffect(apiKey, units, glassSettings.weatherSource) {
        if (glassSettings.weatherSource == "OpenWeather" && apiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {
                WeatherRepository.fetchForecast(0.0, 0.0, units = units, apiKey = apiKey)
            }
        }
    }

    data class Forecast(val label: String, val temp: String, val icon: ImageVector)
    fun mapIconCode(code: String): ImageVector = when {
        code.startsWith("01") -> Icons.Rounded.WbSunny
        code.startsWith("02") || code.startsWith("03") || code.startsWith("04") -> Icons.Rounded.CloudQueue
        code.startsWith("09") || code.startsWith("10") -> Icons.Rounded.Grain
        code.startsWith("11") -> Icons.Rounded.FlashOn
        code.startsWith("13") -> Icons.Rounded.AcUnit
        else -> Icons.Rounded.Cloud
    }

    val forecasts = remember(fetched) {
        fetched?.hourly?.map { Forecast(it.label, it.temp, mapIconCode(it.iconCode)) } ?: listOf(
            Forecast("Now", "--°", Icons.Rounded.Cloud),
            Forecast("+3h", "--°", Icons.Rounded.Cloud)
        )
    }

    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(forecasts.size) {
        while (forecasts.size > 1) {
            delay(4000L)
            index = (index + 1) % forecasts.size
        }
    }

    val xOffset by animateFloatAsState(
        targetValue = -index * 120f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "weatherAnimation"
    )

    val current = forecasts.getOrNull(index) ?: forecasts[0]

    if (style == "Compact") {
        // Minimal glance: icon + temperature only, no forecast strip or timestamp
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = current.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(text = current.temp, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                if (fetched?.location != null) {
                    Text(
                        text = fetched?.location ?: "",
                        color = Color.White.copy(0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (fetched?.location != null) {
            Text(
                text = fetched?.location ?: "",
                color = Color.White.copy(0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }

        Icon(
            imageVector = current.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(text = current.temp, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
        Text(text = current.label, color = Color.White.copy(0.7f), fontSize = 12.sp)

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(0.03f))
        ) {
            Row(
                modifier = Modifier
                    .offset(x = xOffset.dp)
                    .padding(horizontal = 4.dp)
            ) {
                forecasts.forEach { f ->
                    Row(
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(f.icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(f.label + ": " + f.temp, color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        if (fetched?.lastUpdated != null) {
            Spacer(Modifier.height(4.dp))
            val timeStr = remember(fetched?.lastUpdated) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fetched?.lastUpdated ?: 0L))
            }
            Text(
                text = "Updated: $timeStr",
                color = Color.White.copy(0.4f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun BatteryPanelContent(glassSettings: LiquidGlassSettings, styleOverride: String? = null) {
    val style = styleOverride ?: glassSettings.batteryStyle
    val context = LocalContext.current
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager }
    var level by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
            isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL
            delay(5000)
        }
    }

    val fillWidth by animateFloatAsState(targetValue = level / 100f, animationSpec = tween(800), label = "batteryFill")

    if (style == "Minimal") {
        // Slim horizontal bar + big percentage, no icon canvas
        val fillColor = when {
            level > 80 -> Color(0xFF22C55E)
            level > 20 -> Color(0xFFFBBF24)
            else -> Color(0xFFEF4444)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$level%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (isCharging) {
                Text("Charging", color = Color.White.copy(0.7f), fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillWidth.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(fillColor)
                )
            }
        }
        return
    }

    val idlePulse = rememberInfiniteTransition(label = "batteryPulse")
    val bob by idlePulse.animateFloat(
        0f,
        3f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "batteryBob"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(y = bob.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val capW = w * 0.08f
                val bodyW = w - capW - 6f
                val bodyH = h * 0.5f
                val left = 3f
                val top = (h - bodyH) / 2f

                drawRoundRect(
                    Color.White.copy(0.12f),
                    Offset(left, top),
                    Size(bodyW, bodyH),
                    CornerRadius(10f)
                )

                val fillColor = when {
                    level > 80 -> Color(0xFF22C55E)
                    level > 20 -> Color(0xFFFBBF24)
                    else -> Color(0xFFEF4444)
                }

                drawRoundRect(
                    fillColor,
                    Offset(left, top),
                    Size(bodyW * fillWidth, bodyH),
                    CornerRadius(10f)
                )

                drawRoundRect(
                    Color.White.copy(0.12f),
                    Offset(left + bodyW + 3f, top + bodyH * 0.25f),
                    Size(capW, bodyH * 0.5f),
                    CornerRadius(4f)
                )
            }
        }
        Text(text = "$level%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (isCharging) {
            Text("Charging", color = Color.White.copy(0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
fun QuickSettingsPanelContent() {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickSettingButton(icon = Icons.Rounded.Wifi, enabled = true) {
            try {
                context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            } catch (e: Exception) {
            }
        }
        QuickSettingButton(icon = Icons.Rounded.Bluetooth, enabled = false) {
            try {
                context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (e: Exception) {
            }
        }
        QuickSettingButton(icon = Icons.Rounded.FlashlightOn, enabled = false) {
            // Flashlight toggle logic
        }
        QuickSettingButton(icon = Icons.Rounded.Settings, enabled = false) {
            try {
                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
            }
        }
    }
}

@Composable
private fun QuickSettingButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val view = LocalView.current
    val colors = GlassThemeState.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.primary else Color.White.copy(0.1f))
            .then(
                if (!enabled) Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape) else Modifier
            )
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun MediaControlPanelContent() {
    val mediaState by com.liqora.launcher.services.MediaStateRepository.mediaState.collectAsState()
    val view = LocalView.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (mediaState == null) {
            Text("No Media", color = Color.White.copy(0.4f), fontSize = 12.sp)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    com.liqora.launcher.services.MediaStateRepository.skipToPrevious()
                }) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White)
                }
                Surface(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        com.liqora.launcher.services.MediaStateRepository.playPause()
                    },
                    shape = CircleShape,
                    color = Color.White.copy(0.1f)
                ) {
                    Icon(
                        if (mediaState?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp)
                    )
                }
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    com.liqora.launcher.services.MediaStateRepository.skipToNext()
                }) {
                    Icon(Icons.Rounded.SkipNext, null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                mediaState?.title ?: "",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppGridPanelContent(item: LauncherItem.GlassPanel, glassSettings: LiquidGlassSettings) {
    val context = LocalContext.current
    val apps = item.apps
    if (apps.isEmpty()) {
        Text("Empty", color = Color.White.copy(0.4f), fontSize = 12.sp)
        return
    }

    // Auto-calculate columns based on count to keep icons reasonably sized
    val cols = when {
        apps.size <= 1 -> 1
        apps.size <= 4 -> 2
        apps.size <= 9 -> 3
        else -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(cols),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp), // Internal padding to prevent edge-touching
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(apps) { pkg ->
            val icon = remember(pkg) {
                try {
                    context.packageManager.getApplicationIcon(pkg)
                } catch (e: Exception) {
                    null
                }
            }
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { launchAppLocal(context, pkg) },
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(96, 96).asImageBitmap(),
                        null,
                        modifier = Modifier.fillMaxSize(0.85f) // Better scaling within the subgrid cell
                    )
                } else {
                    Icon(Icons.Rounded.Android, null, tint = Color.White.copy(0.3f))
                }
            }
        }
    }
}

@Composable
fun BrowserSearchPanelContent(openBrowserOnTap: Boolean, isEditMode: Boolean) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val view = LocalView.current

    val searchAction = {
        if (query.isNotEmpty()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
            query = ""
            focusManager.clearFocus()
        } else {
            // If empty, always open browser regardless of setting if explicitly triggered by button
            context.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
        }
    }

    if (openBrowserOnTap && !isEditMode) {
        // Render as a clean button that opens the browser immediately
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                context.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
            },
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Search...",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Rounded.Search,
                    null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        // Standard interactive text field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = { Text("Search...", color = Color.White.copy(0.4f), fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White.copy(0.3f),
                unfocusedBorderColor = Color.White.copy(0.1f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            trailingIcon = {
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    searchAction()
                }) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.6f))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                searchAction()
            }),
            enabled = !isEditMode
        )
    }
}

@Composable
private fun PlayIntegrityPanelContent(glassSettings: LiquidGlassSettings) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.Security,
            null,
            tint = if (glassSettings.playIntegrityEnabled) Color(0xFF6366F1) else Color.White.copy(0.3f)
        )
        Spacer(Modifier.height(4.dp))
        Text("Play Integrity", color = Color.White, fontSize = 12.sp)
        Text(
            if (glassSettings.playIntegrityEnabled) "Verified" else "Disabled",
            color = Color.White.copy(0.5f),
            fontSize = 10.sp
        )
    }
}

private fun launchAppLocal(context: Context, pkg: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) context.startActivity(intent)
    } catch (e: Exception) {
    }
}
