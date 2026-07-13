package com.liqora.launcher.compose.launcher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.liqora.launcher.helpers.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Maps an installed app to a human-friendly category label using the
 * platform's own `ApplicationInfo.category` (declared by the app itself in
 * its manifest, API 26+). Apps that don't declare one fall into "Other".
 * Results are cached in memory — the same package is never re-queried.
 */
private object AppCategoryCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Preferred display order; anything else (rare) is appended alphabetically after. */
    val ORDER = listOf(
        "Social", "Productivity", "Games", "Photography",
        "Video & Entertainment", "Music & Audio", "News", "Maps & Navigation", "Other"
    )

    fun categoryFor(context: android.content.Context, packageName: String): String {
        cache[packageName]?.let { return it }
        val label = try {
            when (context.packageManager.getApplicationInfo(packageName, 0).category) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Music & Audio"
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Video & Entertainment"
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Photography"
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Maps & Navigation"
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> "Other"
            }
        } catch (e: Exception) {
            "Other"
        }
        cache[packageName] = label
        return label
    }
}

/**
 * Per-category visual identity — icon + accent color, matching the
 * iPhone-style category grid (each category gets its own hue instead
 * of one shared panel tint).
 */
private object CategoryStyle {
    data class Style(val icon: ImageVector, val color: Color)

    private val styles = mapOf(
        "All Apps" to Style(Icons.Rounded.Apps, Color(0xFF6C63FF)),
        "Social" to Style(Icons.Rounded.Group, Color(0xFF2ECC71)),
        "Productivity" to Style(Icons.Rounded.Work, Color(0xFF1ABC9C)),
        "Games" to Style(Icons.Rounded.VideogameAsset, Color(0xFF9B59B6)),
        "Photography" to Style(Icons.Rounded.PhotoCamera, Color(0xFFF39C12)),
        "Video & Entertainment" to Style(Icons.Rounded.Movie, Color(0xFFE84393)),
        "Music & Audio" to Style(Icons.Rounded.MusicNote, Color(0xFFE67E22)),
        "News" to Style(Icons.Rounded.Article, Color(0xFF3498DB)),
        "Maps & Navigation" to Style(Icons.Rounded.Map, Color(0xFF16A085)),
        "Other" to Style(Icons.Rounded.Widgets, Color(0xFF7F8C8D))
    )

    // Any category not in the fixed map (rare — non-standard label) gets a
    // stable color derived from its own hash so it's at least consistent
    // across recompositions instead of always falling back to grey.
    fun styleFor(label: String): Style = styles[label] ?: Style(
        Icons.Rounded.Widgets,
        Color.hsv((label.hashCode().mod(360)).toFloat(), 0.45f, 0.55f)
    )
}

/**
 * Reads real "recently opened" apps from the system's usage stats, so the
 * Recent tab isn't empty until the user launches something from inside the
 * drawer itself. Requires the PACKAGE_USAGE_STATS special permission, which
 * (like on stock Android) can only be granted via the system Usage Access
 * settings screen — there's no runtime permission dialog for it.
 */
private object RecentAppsTracker {

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Most-recently-foregrounded packages first. Call off the main thread. */
    fun queryRecentPackages(context: Context, lookbackDays: Int = 7, limit: Int = 16): List<String> {
        if (!hasPermission(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - lookbackDays * 24L * 60L * 60L * 1000L

        val lastUsed = LinkedHashMap<String, Long>()
        val event = UsageEvents.Event()
        val events = usm.queryEvents(start, end)
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastUsed[event.packageName] = event.timeStamp
            }
        }
        return lastUsed.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    data class AppUsageSummary(
        val totalForegroundMillis: Long,
        val launchCount: Int,
        val dailyMinutes: List<Pair<String, Int>> // day label ("Mon") to minutes, oldest first
    )

    /** Total foreground time + launch count + per-day breakdown for the given
     *  package over the last [days] days. Call off the main thread. */
    fun queryAppUsageSummary(context: Context, packageName: String, days: Int = 7): AppUsageSummary {
        if (!hasPermission(context)) return AppUsageSummary(0L, 0, emptyList())
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = java.util.Calendar.getInstance()
        val end = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        val millisPerDay = 24L * 60L * 60L * 1000L
        val dailyMillis = LongArray(days)
        var launchCount = 0
        var totalMillis = 0L
        var lastForegroundStart: Long? = null

        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    launchCount++
                    lastForegroundStart = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    lastForegroundStart?.let { fgStart ->
                        addSegmentToBuckets(dailyMillis, fgStart, event.timeStamp, start, millisPerDay)
                        totalMillis += (event.timeStamp - fgStart).coerceAtLeast(0)
                        lastForegroundStart = null
                    }
                }
            }
        }
        // Agar app abhi bhi foreground me hai (query end tak), us segment ko bhi close kar
        lastForegroundStart?.let { fgStart ->
            addSegmentToBuckets(dailyMillis, fgStart, end, start, millisPerDay)
            totalMillis += (end - fgStart).coerceAtLeast(0)
        }

        val dayFormatter = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val dailyMinutes = (0 until days).map { i ->
            val dayStart = start + i * millisPerDay
            dayFormatter.format(java.util.Date(dayStart)) to (dailyMillis[i] / 60000L).toInt()
        }

        return AppUsageSummary(totalMillis, launchCount, dailyMinutes)
    }

    private fun addSegmentToBuckets(buckets: LongArray, segStart: Long, segEnd: Long, rangeStart: Long, millisPerDay: Long) {
        var cur = segStart.coerceAtLeast(rangeStart)
        if (cur >= segEnd) return
        while (cur < segEnd) {
            val dayIndex = ((cur - rangeStart) / millisPerDay).toInt()
            if (dayIndex < 0 || dayIndex >= buckets.size) break
            val dayEnd = rangeStart + (dayIndex + 1) * millisPerDay
            val segmentEndInDay = minOf(segEnd, dayEnd)
            buckets[dayIndex] += (segmentEndInDay - cur)
            cur = segmentEndInDay
        }
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}


private enum class DrawerTab { CATEGORIES, RECENT }

/** How the category boxes screen is laid out — full-width scrolling list, or a 2-column grid. */
private enum class CategoryViewMode { LIST, GRID }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppDrawer(
    apps: List<AvailableApp>,
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    onAppClick: (String) -> Unit,
    onClose: () -> Unit,
    onRefreshApps: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Prewarm cache when apps or settings change
    LaunchedEffect(apps, glassSettings.iconPackPackageName, glassSettings.useIconPackInAppDrawer) {
        val requests = apps.map { app ->
            AppIconCache.IconRequest(
                component = app.componentName,
                customIconUri = app.customIconUri,
                iconPackPackage = if (glassSettings.useIconPackInAppDrawer) glassSettings.iconPackPackageName else null
            )
        }
        AppIconCache.prewarmMemoryCache(context, requests, 192)
    }

    // Ensure state updates trigger recomposition of filtered list
    val currentApps = rememberUpdatedState(apps)

    // State for app options dialog
    var appForOptions by remember { mutableStateOf<AvailableApp?>(null) }
    var appForUsageStats by remember { mutableStateOf<AvailableApp?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Bottom bar tab state + recently-launched packages (most recent first,
    // capped so it never grows unbounded). This is session-only — plug in
    // a persisted store later if you want it to survive process death.
    var selectedMainTab by remember { mutableStateOf(DrawerTab.CATEGORIES) }
    var recentPackages by remember { mutableStateOf<List<String>>(emptyList()) }
    var categoryViewMode by remember { mutableStateOf(CategoryViewMode.LIST) }
    var hasUsagePermission by remember { mutableStateOf(RecentAppsTracker.hasPermission(context)) }

    // Seed Recent with real system usage history as soon as the drawer opens,
    // so it isn't empty until the user launches something from here first.
    // Session launches (handleAppClick below) still get merged in and bumped
    // to the front on top of this.
    LaunchedEffect(Unit) {
        hasUsagePermission = RecentAppsTracker.hasPermission(context)
        if (hasUsagePermission) {
            val seeded = withContext(Dispatchers.IO) { RecentAppsTracker.queryRecentPackages(context) }
            if (seeded.isNotEmpty()) {
                recentPackages = (recentPackages + seeded.filterNot { it in recentPackages })
            }
        }
    }

    val filteredApps = remember(currentApps.value, searchQuery) {
        if (searchQuery.isBlank()) currentApps.value
        else currentApps.value.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    // Wraps the real onAppClick so every launch also updates the Recent list.
    val handleAppClick: (AvailableApp) -> Unit = { app ->
        recentPackages = (listOf(app.packageName) + recentPackages.filterNot { it == app.packageName }).take(16)
        onAppClick(app.packageName)
    }

    // Category-wise browsing — computed off the main thread once per app-list
    // change (results are cached per-package so this is cheap on repeat).
    // Category chips are only meaningful while not searching; search always
    // looks across every app regardless of the selected chip.
    var categoryOfApp by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentApps.value) {
        val map = withContext(Dispatchers.IO) {
            currentApps.value.associate { it.packageName to AppCategoryCache.categoryFor(context, it.packageName) }
        }
        categoryOfApp = map
    }
    val availableCategories = remember(categoryOfApp) {
        val present = categoryOfApp.values.toSet()
        AppCategoryCache.ORDER.filter { it in present } + (present - AppCategoryCache.ORDER.toSet()).sorted()
    }
    val visibleApps = remember(filteredApps, selectedCategory, categoryOfApp, searchQuery, selectedMainTab, recentPackages) {
        when {
            searchQuery.isNotBlank() -> filteredApps
            selectedMainTab == DrawerTab.RECENT ->
                recentPackages.mapNotNull { pkg -> filteredApps.find { it.packageName == pkg } }
            selectedCategory == null -> filteredApps
            else -> filteredApps.filter { categoryOfApp[it.packageName] == selectedCategory }
        }
    }

    // Use BoxWithConstraints to get accurate height for anchors
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()

        // Physics-based swipe-to-dismiss
        // 1 = Open (offset 0), 0 = Closed (offset screenHeightPx)
        val swipeableState = rememberSwipeableState(initialValue = 0)
        val anchors = mapOf(0f to 1, screenHeightPx to 0)
        var isInitialized by remember { mutableStateOf(false) }

        // Start animation to "Open" state
        LaunchedEffect(screenHeightPx) {
            if (screenHeightPx > 0 && !isInitialized) {
                swipeableState.animateTo(1)
                isInitialized = true
            }
        }

        // Detect if closed by swipe (only after initialization)
        LaunchedEffect(swipeableState.currentValue) {
            if (isInitialized && swipeableState.currentValue == 0) {
                onClose()
            }
        }

        // Fail-safe: Detect if visually closed (offset at bottom) to ensure state sync
        LaunchedEffect(swipeableState, screenHeightPx, isInitialized) {
            snapshotFlow { swipeableState.offset.value }
                .collect { offset ->
                    if (isInitialized && offset >= screenHeightPx - 2f && screenHeightPx > 0) {
                        onClose()
                    }
                }
        }

        // Determine panel style
        val panelColor = Color(glassSettings.panelTintColor)
        val panelAlpha = glassSettings.drawerBackgroundAlpha
        val blurRadius = glassSettings.drawerBlurRadius.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .graphicsLayer { translationY = swipeableState.offset.value }
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.25f) },
                    orientation = Orientation.Vertical
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .then(
                    if (glassSettings.drawerBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(glassSettings.panelCornerRadius.dp) },
                            effects = {
                                if (glassSettings.vibrancyEnabled) vibrancy()
                                blur(blurRadius.toPx())
                                if (glassSettings.lensEnabled) lens(
                                    refractionHeight = glassSettings.refractionHeight.dp.toPx(),
                                    refractionAmount = glassSettings.refractionAmount.dp.toPx(),
                                    chromaticAberration = glassSettings.chromaticAberration
                                )
                            },
                            onDrawSurface = {
                                drawRect(panelColor.copy(alpha = panelAlpha))
                            }
                        )
                    } else {
                        Modifier.background(
                            color = Color.Black.copy(alpha = panelAlpha),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                    }
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    // Prevent clicks passing through and clear focus
                    focusManager.clearFocus()
                }
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            // Drag Handle Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LiquidGlassSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .background(GlassThemeState.colors.textSecondary.copy(alpha = 0.35f), CircleShape)
                )
            }


            val drawerColors = GlassThemeState.colors
            var searchFocused by remember { mutableStateOf(false) }
            val searchShape = LiquidGlassRadius.shapePill

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(searchShape)
                    .then(
                        if (glassSettings.drawerBlurEnabled) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(9999f) },
                                effects = {
                                    if (glassSettings.vibrancyEnabled) vibrancy()
                                    blur(glassSettings.drawerBlurRadius.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(panelColor.copy(alpha = (panelAlpha + 0.14f).coerceIn(0f, 1f)))
                                }
                            )
                        } else {
                            Modifier.background(panelColor.copy(alpha = (panelAlpha + 0.14f).coerceIn(0f, 1f)), searchShape)
                        }
                    )
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { searchFocused = it.isFocused },
                    placeholder = {
                        Text(
                            "Search apps",
                            style = LiquidGlassTypography.body,
                            color = drawerColors.textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = if (searchFocused) drawerColors.primary else drawerColors.textSecondary
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = drawerColors.textSecondary
                                )
                            }
                        }
                    } else null,
                    textStyle = LiquidGlassTypography.body.copy(color = drawerColors.textPrimary),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = drawerColors.textPrimary,
                        unfocusedTextColor = drawerColors.textPrimary,
                        focusedBorderColor = drawerColors.primary.copy(alpha = 0.65f),
                        unfocusedBorderColor = drawerColors.glassBorder,
                        cursorColor = drawerColors.primary,
                        // Transparent rakha hai — asli blur peeche wale Box se aa raha hai
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = searchShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            }
            Spacer(modifier = Modifier.height(LiquidGlassSpacing.sm))

            // App Grid
            val listState = rememberLazyGridState()
            val boxGridState = rememberLazyGridState()
            // Whether we've drilled into "All Apps" from the category boxes
            // screen (as opposed to a specific category) — kept separate from
            // selectedCategory so visibleApps' existing "null = everything"
            // logic doesn't need to change.
            var viewingAllFlat by remember { mutableStateOf(false) }
            val showCategoryBoxes = searchQuery.isBlank() &&
                selectedMainTab == DrawerTab.CATEGORIES &&
                selectedCategory == null &&
                !viewingAllFlat

            // Nested scroll to detect overscroll at top
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    // We only want to handle FLING here.
                    // Drag is handled by the parent swipeable modifier if the child (LazyGrid)
                    // cannot scroll further up.
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (available.y < 0 && swipeableState.offset.value > 0f) {
                            scope.launch {
                                swipeableState.performDrag(available.y)
                            }
                            return available
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (available.y > 0 && source == NestedScrollSource.Drag) {
                            scope.launch {
                                swipeableState.performDrag(available.y)
                            }
                            return available
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (swipeableState.offset.value > 0f) {
                            swipeableState.performFling(available.y)
                            return available
                        }
                        return Velocity.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        return Velocity.Zero
                    }
                }
            }

            if (showCategoryBoxes) {
                // ---- iPhone App Library–style category boxes (the default view) ----
                // Two layouts available via the toggle below:
                //  - LIST: full-width rows (CategoryBoxCard) — plenty of room for
                //    label + count + 4-icon preview, this is what fixed the earlier
                //    truncation/overlap bug.
                //  - GRID: compact 2-column tiles (CategoryGridCard) — a lighter,
                //    purpose-built card (no 2x2 cluster) so it never hits that same
                //    width squeeze.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = LiquidGlassTypography.footnote,
                        color = drawerColors.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .clip(LiquidGlassRadius.shapePill)
                            .background(drawerColors.glassSurface, LiquidGlassRadius.shapePill)
                            .border(1.dp, drawerColors.glassBorder, LiquidGlassRadius.shapePill)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ViewModeToggleButton(
                            icon = Icons.Rounded.ViewList,
                            contentDescription = "List view",
                            selected = categoryViewMode == CategoryViewMode.LIST,
                            colors = drawerColors,
                            onClick = { categoryViewMode = CategoryViewMode.LIST }
                        )
                        ViewModeToggleButton(
                            icon = Icons.Rounded.GridView,
                            contentDescription = "Grid view",
                            selected = categoryViewMode == CategoryViewMode.GRID,
                            colors = drawerColors,
                            onClick = { categoryViewMode = CategoryViewMode.GRID }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LiquidGlassSpacing.xs))

                Box(modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        state = boxGridState,
                        columns = GridCells.Fixed(if (categoryViewMode == CategoryViewMode.LIST) 1 else 2),
                        verticalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.sm),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        item(key = "cat_all_apps") {
                            if (categoryViewMode == CategoryViewMode.LIST) {
                                CategoryBoxCard(
                                    label = "All Apps",
                                    apps = filteredApps,
                                    glassSettings = glassSettings,
                                    backdrop = backdrop,
                                    onClick = { viewingAllFlat = true }
                                )
                            } else {
                                CategoryGridCard(
                                    label = "All Apps",
                                    apps = filteredApps,
                                    glassSettings = glassSettings,
                                    backdrop = backdrop,
                                    onClick = { viewingAllFlat = true }
                                )
                            }
                        }
                        items(availableCategories, key = { it }) { category ->
                            val apps = remember(filteredApps, categoryOfApp, category) {
                                filteredApps.filter { categoryOfApp[it.packageName] == category }
                            }
                            if (categoryViewMode == CategoryViewMode.LIST) {
                                CategoryBoxCard(
                                    label = category,
                                    apps = apps,
                                    glassSettings = glassSettings,
                                    backdrop = backdrop,
                                    onClick = { selectedCategory = category }
                                )
                            } else {
                                CategoryGridCard(
                                    label = category,
                                    apps = apps,
                                    glassSettings = glassSettings,
                                    backdrop = backdrop,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }
                    }
                }
            } else {
                // ---- Drilled-in flat grid: a category, "All Apps", Recent, or search results ----
                val drilledCategory = selectedCategory
                val showCategoryBanner = drilledCategory != null &&
                    searchQuery.isBlank() &&
                    selectedMainTab == DrawerTab.CATEGORIES

                if (showCategoryBanner) {
                    CategoryHeaderBanner(
                        label = drilledCategory!!,
                        count = visibleApps.size,
                        colors = drawerColors,
                        onBack = { selectedCategory = null; viewingAllFlat = false }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val showBack = searchQuery.isBlank() &&
                                selectedMainTab == DrawerTab.CATEGORIES &&
                                viewingAllFlat
                            if (showBack) {
                                IconButton(
                                    onClick = { selectedCategory = null; viewingAllFlat = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to categories",
                                        tint = drawerColors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "Results"
                                    selectedMainTab == DrawerTab.RECENT -> "Recent"
                                    else -> "All Apps"
                                },
                                style = LiquidGlassTypography.footnote,
                                color = drawerColors.textSecondary
                            )
                        }
                        Text(
                            text = "${visibleApps.size}",
                            style = LiquidGlassTypography.footnote,
                            color = drawerColors.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LiquidGlassSpacing.xs))

                Box(modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Fixed(glassSettings.gridColumns),
                        verticalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.xs),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppDrawerItem(
                                app = app,
                                glassSettings = glassSettings,
                                onClick = {
                                    // Recent tab me tap = usage stats dialog; baaki jagah tap = launch
                                    if (selectedMainTab == DrawerTab.RECENT && searchQuery.isBlank()) {
                                        appForUsageStats = app
                                    } else {
                                        handleAppClick(app)
                                    }
                                },
                                onLongClick = { appForOptions = app }
                            )
                        }
                    }

                    if (visibleApps.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (selectedMainTab == DrawerTab.RECENT && searchQuery.isBlank())
                                    Icons.Rounded.History
                                else Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = drawerColors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(LiquidGlassSpacing.sm))
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "No apps match \"$searchQuery\""
                                    selectedMainTab == DrawerTab.RECENT && !hasUsagePermission ->
                                        "Grant Usage Access to see recently opened apps here"
                                    selectedMainTab == DrawerTab.RECENT -> "No recently opened apps yet"
                                    else -> "No apps in ${selectedCategory ?: "this category"}"
                                },
                                style = LiquidGlassTypography.subheadline,
                                color = drawerColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (selectedMainTab == DrawerTab.RECENT && searchQuery.isBlank() && !hasUsagePermission) {
                                Spacer(Modifier.height(LiquidGlassSpacing.sm))
                                TextButton(onClick = { RecentAppsTracker.openUsageAccessSettings(context) }) {
                                    Text("Open Settings", color = drawerColors.primary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(LiquidGlassSpacing.sm))


            DrawerBottomBar(
                backdrop = backdrop,
                glassSettings = glassSettings,
                panelColor = panelColor,
                panelAlpha = panelAlpha,
                selectedTab = selectedMainTab,
                onSelectRecent = {
                    selectedMainTab = DrawerTab.RECENT
                    selectedCategory = null
                    viewingAllFlat = false
                    focusManager.clearFocus()
                },
                onSelectCategories = {
                    selectedMainTab = DrawerTab.CATEGORIES
                    selectedCategory = null
                    viewingAllFlat = false
                    focusManager.clearFocus()
                },
                onSelectSettings = onOpenSettings
            )
        }

        if (appForOptions != null && !showEditDialog) {
            AppOptionsDialog(
                app = appForOptions!!,
                backdrop = backdrop,
                glassSettings = glassSettings,
                panelColor = panelColor,
                panelAlpha = panelAlpha,
                onDismiss = { appForOptions = null },
                onEdit = { showEditDialog = true },
                onKill = {
                    val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses(appForOptions!!.packageName)
                    android.widget.Toast.makeText(context, "Killed ${appForOptions!!.label}", android.widget.Toast.LENGTH_SHORT).show()
                    appForOptions = null
                },
                onUninstall = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DELETE)
                    intent.data = android.net.Uri.parse("package:${appForOptions!!.packageName}")
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    appForOptions = null
                },
                onUsageAccess = {
                    appForUsageStats = appForOptions
                    appForOptions = null
                }
            )
        }

        if (showEditDialog && appForOptions != null) {
            EditAppInfoDialog(
                app = appForOptions!!,
                backdrop = backdrop,
                glassSettings = glassSettings,
                panelColor = panelColor,
                panelAlpha = panelAlpha,
                onDismiss = { showEditDialog = false; appForOptions = null },
                onSave = { newLabel, newIconUri ->
                    scope.launch(Dispatchers.IO) {
                        val metadata = AppMetadata(
                            label = if (newLabel != appForOptions!!.label) newLabel else null,
                            customIconUri = newIconUri
                        )
                        AppMetadataRepository.saveMetadata(context, appForOptions!!.packageName, metadata)
                        withContext(Dispatchers.Main) {
                            onRefreshApps()
                            showEditDialog = false
                            appForOptions = null
                        }
                    }
                }
            )
        }


        if (appForUsageStats != null) {
            AppUsageStatsDialog(
                app = appForUsageStats!!,
                backdrop = backdrop,
                glassSettings = glassSettings,
                panelColor = panelColor,
                panelAlpha = panelAlpha,
                onDismiss = { appForUsageStats = null }
            )
        }
    }








}

/**
 * Bottom navigation bar for the drawer: Recent, Categories, Settings.
 * Recent/Categories swap what's shown above; Settings just forwards to
 * onSelectSettings since it's expected to navigate to its own screen.
 *
 * Uses the same panelColor/panelAlpha (glassSettings.panelTintColor +
 * drawerBackgroundAlpha) as the main drawer panel — so it always matches
 * whatever Light/Dark tint the user has picked — and, like the category
 * cards, skips its own live drawBackdrop blur. The one blur pass on the
 * drawer panel itself already sits behind this bar, so a second independent
 * blur here was pure redundant GPU cost and part of the lag.
 */
@Composable
private fun DrawerBottomBar(
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    panelColor: Color,
    panelAlpha: Float,
    selectedTab: DrawerTab,
    onSelectRecent: () -> Unit,
    onSelectCategories: () -> Unit,
    onSelectSettings: () -> Unit
) {
    val colors = GlassThemeState.colors
    val shape = LiquidGlassRadius.shapeLg
    val blurRadius = glassSettings.drawerBlurRadius.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .then(
                if (glassSettings.drawerBlurEnabled) {
                    // Panel jaisa hi real backdrop blur — same backdrop source,
                    // isliye visually panel ke saath seamlessly match karega.
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(LiquidGlassRadius.shapeLg.let { 16f } ) },
                        effects = {
                            if (glassSettings.vibrancyEnabled) vibrancy()
                            blur(blurRadius.toPx())
                        },
                        onDrawSurface = {
                            drawRect(panelColor.copy(alpha = (panelAlpha + 0.10f).coerceIn(0f, 1f)))
                        }
                    )
                } else {
                    Modifier.background(panelColor.copy(alpha = (panelAlpha + 0.10f).coerceIn(0f, 1f)), shape)
                }
            )
            .border(1.dp, colors.glassBorder, shape),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawerBottomBarItem(
            icon = Icons.Rounded.History,
            label = "Recent",
            selected = selectedTab == DrawerTab.RECENT,
            colors = colors,
            onClick = onSelectRecent
        )
        DrawerBottomBarItem(
            icon = Icons.Rounded.Apps,
            label = "Categories",
            selected = selectedTab == DrawerTab.CATEGORIES,
            colors = colors,
            onClick = onSelectCategories
        )
        DrawerBottomBarItem(
            icon = Icons.Rounded.Settings,
            label = "Settings",
            selected = false,
            colors = colors,
            onClick = onSelectSettings
        )
    }
}
@Composable
private fun RowScope.DrawerBottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    colors: LiquidGlassColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (selected) colors.primary else colors.textSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = LiquidGlassTypography.caption,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ViewModeToggleButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    colors: LiquidGlassColors,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(if (selected) colors.primary.copy(alpha = 0.22f) else Color.Transparent, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) colors.primary else colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Colored banner shown when the user has drilled into a specific category —
 * replaces the plain text header with something that actually feels like
 * "you're inside Games now" instead of a generic list title.
 */
@Composable
private fun CategoryHeaderBanner(
    label: String,
    count: Int,
    colors: LiquidGlassColors,
    onBack: () -> Unit
) {
    val style = remember(label) { CategoryStyle.styleFor(label) }
    val shape = LiquidGlassRadius.shapeLg

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(style.color.copy(alpha = 0.18f), shape)
            .border(1.dp, style.color.copy(alpha = 0.35f), shape)
            .padding(horizontal = LiquidGlassSpacing.sm, vertical = LiquidGlassSpacing.xs)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to categories",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(style.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(LiquidGlassSpacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = LiquidGlassTypography.callout,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count apps",
                style = LiquidGlassTypography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
/**
 * A single category tile: colored icon chip on the left, label + count in
 * the middle (each capped to one line with ellipsis so long names can never
 * wrap and overlap the row below), and a 2x2 preview cluster of that
 * category's own app icons on the right. Now rendered as a full-width row
 * (single-column grid) so there's always enough horizontal room for all
 * three pieces — the old 2-column layout left only a couple dp for the
 * label, which is what caused the truncation/overlap.
 */
private fun CategoryBoxCard(
    label: String,
    apps: List<AvailableApp>,
    glassSettings: LiquidGlassSettings,
    backdrop: LayerBackdrop,
    onClick: () -> Unit
) {
    val colors = GlassThemeState.colors
    val shape = LiquidGlassRadius.shapeLg
    val style = remember(label) { CategoryStyle.styleFor(label) }
    val previewApps = remember(apps) { apps.sortedBy { it.label.lowercase() }.take(4) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "categoryCardPressScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            // NOTE: this used to run a real-time drawBackdrop blur on every
            // single card (10+ cards on screen at once, each with its own
            // blur shader pass). That's what was causing the heavy lag on
            // this screen — GPU was re-blurring the whole panel N times per
            // frame while scrolling. A flat tinted background gives the same
            // "glass" identity (via the category's own accent color) at a
            // fraction of the cost. Live blur is kept only on the single
            // drawer panel itself and the bottom bar, not per-item.
            .background(style.color.copy(alpha = 0.16f), shape)
            .border(1.dp, style.color.copy(alpha = 0.35f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = LiquidGlassSpacing.md, vertical = LiquidGlassSpacing.sm)
    ) {
        // Solid colored icon chip identifying the category
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(style.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                style.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(LiquidGlassSpacing.md))

        // weight(1f) now has real room to breathe since the card is full-width,
        // but we still cap both lines to a single line each as a hard guard —
        // this is what actually prevents the count text wrapping into and
        // overlapping the next card.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = LiquidGlassTypography.callout,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${apps.size} apps",
                style = LiquidGlassTypography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(LiquidGlassSpacing.sm))

        // 2x2 mini icon cluster — falls back to a soft placeholder square for
        // any of the 4 slots that don't have an app (keeps every card the
        // same shape instead of icons re-centering as counts change).
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        if (index < previewApps.size) {
                            MiniAppIcon(app = previewApps[index], glassSettings = glassSettings)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(colors.glassSurface.copy(alpha = colors.glassSurface.alpha * 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact 2-column tile used in Grid view. Deliberately simpler than
 * CategoryBoxCard — icon on top, single-line label + count below, and a
 * single row of up to 3 small preview icons (not a 2x2 cluster). At half
 * screen width a 2x2 cluster is exactly what caused the original overlap
 * bug, so this card never attempts one.
 */
@Composable
private fun CategoryGridCard(
    label: String,
    apps: List<AvailableApp>,
    glassSettings: LiquidGlassSettings,
    backdrop: LayerBackdrop,
    onClick: () -> Unit
) {
    val colors = GlassThemeState.colors
    val shape = LiquidGlassRadius.shapeLg
    val style = remember(label) { CategoryStyle.styleFor(label) }
    val previewApps = remember(apps) { apps.sortedBy { it.label.lowercase() }.take(3) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "categoryGridCardPressScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            // Same reasoning as CategoryBoxCard: no per-item live blur, flat
            // tinted background instead — this is what fixes the lag.
            .background(style.color.copy(alpha = 0.16f), shape)
            .border(1.dp, style.color.copy(alpha = 0.35f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(LiquidGlassSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(style.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(LiquidGlassSpacing.xs))

        Text(
            text = label,
            style = LiquidGlassTypography.callout,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${apps.size} apps",
            style = LiquidGlassTypography.caption,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (previewApps.isNotEmpty()) {
            Spacer(Modifier.height(LiquidGlassSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                previewApps.forEach { app ->
                    MiniAppIcon(app = app, glassSettings = glassSettings)
                }
            }
        }
    }
}

/** Small (28dp) icon used inside a CategoryBoxCard's preview cluster. Reads
 *  from the same memory-cache-first path as the full-size AppDrawerItem, so
 *  by the time the box grid renders (after the drawer's prewarm pass) these
 *  are effectively free. */
@Composable
private fun MiniAppIcon(app: AvailableApp, glassSettings: LiquidGlassSettings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val iconPack = if (glassSettings.useIconPackInAppDrawer) glassSettings.iconPackPackageName else null

    val memoryIcon = remember(app, iconPack) {
        AppIconCache.getIconFromMemory(context, app.componentName, 96, app.customIconUri, iconPack)
    }
    val iconBitmap = if (memoryIcon != null) {
        remember { mutableStateOf(memoryIcon.asImageBitmap()) }
    } else {
        produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, app.componentName, app.customIconUri, iconPack) {
            withContext(Dispatchers.IO) {
                value = AppIconCache.loadIcon(context, app.componentName, 96, app.customIconUri, iconPack)?.asImageBitmap()
            }
        }
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(GlassThemeState.colors.glassSurface),
        contentAlignment = Alignment.Center
    ) {
        iconBitmap.value?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize().padding(2.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
    app: AvailableApp,
    glassSettings: LiquidGlassSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cornerRadius = glassSettings.iconCornerRadius.dp
    val tintColor = Color(glassSettings.panelTintColor)

    // NOTE: this used to also run a per-item "entrance" spring (scale/alpha)
    // on every composition. Because LazyVerticalGrid disposes item state once
    // it scrolls out of the retained window, that animation was restarting
    // for every item on *every* scroll pass — the actual cause of the drawer
    // feeling laggy on a big app list. Keeping only the press feedback here
    // (cheap, and only active while a finger is actually down) fixes that
    // while still feeling responsive.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "appPressScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                color = tintColor.copy(alpha = glassSettings.iconBackgroundAlpha),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(8.dp)
    ) {
        val iconPack = if (glassSettings.useIconPackInAppDrawer) glassSettings.iconPackPackageName else null

        // Fast path: Check memory cache first
        val memoryIcon = remember(app, iconPack) {
            AppIconCache.getIconFromMemory(context, app.componentName, 192, app.customIconUri, iconPack)
        }

        val iconBitmap = if (memoryIcon != null) {
            remember { androidx.compose.runtime.mutableStateOf(memoryIcon.asImageBitmap()) }
        } else {
            produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, app.componentName, app.customIconUri, iconPack) {
                withContext(Dispatchers.IO) {
                    val loaded = AppIconCache.loadIcon(context, app.componentName, 192, app.customIconUri, iconPack)
                    value = loaded?.asImageBitmap()
                }
            }
        }

        // Icon content - centered
        Box(
            modifier = Modifier
                .fillMaxSize(if (glassSettings.showAppLabels) 0.6f else 0.8f)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap.value != null) {
                Image(
                    bitmap = iconBitmap.value!!,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback icon (should be very rare now)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlassThemeState.colors.glassSurface, RoundedCornerShape(8.dp))
                )
            }
        }

        if (glassSettings.showAppLabels) {
            Text(
                text = app.label,
                style = LiquidGlassTypography.caption.copy(
                    color = Color.White,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                        blurRadius = 3f
                    )
                ),
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
        }
    }
}


@Composable
private fun AppUsageStatsDialog(
    app: AvailableApp,
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    panelColor: Color,
    panelAlpha: Float,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = GlassThemeState.colors
    val hasPermission = remember { RecentAppsTracker.hasPermission(context) }
    var summary by remember { mutableStateOf<RecentAppsTracker.AppUsageSummary?>(null) }
    var isLoading by remember { mutableStateOf(hasPermission) }

    LaunchedEffect(app.packageName) {
        if (hasPermission) {
            summary = withContext(Dispatchers.IO) {
                RecentAppsTracker.queryAppUsageSummary(context, app.packageName, days = 7)
            }
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
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
                .padding(LiquidGlassSpacing.lg)
        ) {
            // Header: icon + name + package
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniAppIcon(app = app, glassSettings = glassSettings)
                Spacer(Modifier.width(LiquidGlassSpacing.sm))
                Column {
                    Text(app.label, style = LiquidGlassTypography.callout, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.packageName, style = LiquidGlassTypography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(LiquidGlassSpacing.md))

            when {
                !hasPermission -> {
                    Text(
                        "Grant Usage Access to see this app's usage stats",
                        style = LiquidGlassTypography.subheadline,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(LiquidGlassSpacing.sm))
                    TextButton(onClick = { RecentAppsTracker.openUsageAccessSettings(context) }) {
                        Text("Open Settings", color = colors.primary)
                    }
                }
                isLoading -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                else -> {
                    val s = summary ?: RecentAppsTracker.AppUsageSummary(0, 0, emptyList())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassSpacing.sm)
                    ) {
                        StatChip(label = "Total time", value = RecentAppsTracker.formatDuration(s.totalForegroundMillis), colors = colors, modifier = Modifier.weight(1f))
                        StatChip(label = "Opened", value = "${s.launchCount}×", colors = colors, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(LiquidGlassSpacing.md))

                    Text("Last 7 days", style = LiquidGlassTypography.footnote, color = colors.textSecondary)
                    Spacer(Modifier.height(LiquidGlassSpacing.xs))
                    UsageBarChart(data = s.dailyMinutes, colors = colors)
                }
            }

            Spacer(Modifier.height(LiquidGlassSpacing.md))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close", color = colors.primary)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, colors: LiquidGlassColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassSurface)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
            .padding(vertical = LiquidGlassSpacing.sm, horizontal = LiquidGlassSpacing.sm)
    ) {
        Text(value, style = LiquidGlassTypography.subheadline, color = colors.textPrimary, maxLines = 1)
        Text(label, style = LiquidGlassTypography.caption, color = colors.textSecondary, maxLines = 1)
    }
}

/** Simple dependency-free bar chart — a Row of weighted Boxes whose height
 *  scales against the day with the most usage. No chart library needed. */
@Composable
private fun UsageBarChart(data: List<Pair<String, Int>>, colors: LiquidGlassColors) {
    val maxMinutes = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, minutes) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val heightFraction = (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height((88.dp * heightFraction).coerceAtLeast(if (minutes > 0) 4.dp else 1.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (minutes > 0) colors.primary else colors.glassSurface)
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = LiquidGlassTypography.caption, color = colors.textSecondary)
            }
        }
    }
}

