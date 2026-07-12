package com.liqora.launcher.compose.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppDrawer(
    apps: List<AvailableApp>,
    backdrop: LayerBackdrop,
    glassSettings: LiquidGlassSettings,
    onAppClick: (String) -> Unit,
    onClose: () -> Unit,
    onRefreshApps: () -> Unit
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
    var showEditDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredApps = remember(currentApps.value, searchQuery) {
        if (searchQuery.isBlank()) currentApps.value
        else currentApps.value.filter { it.label.contains(searchQuery, ignoreCase = true) }
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
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
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

            // Search Bar — premium glass pill, matches the rest of the app instead of a generic outlined field
            val drawerColors = GlassThemeState.colors
            var searchFocused by remember { mutableStateOf(false) }
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
                    focusedContainerColor = drawerColors.glassSurface,
                    unfocusedContainerColor = drawerColors.glassSurface.copy(alpha = drawerColors.glassSurface.alpha * 0.7f)
                ),
                shape = LiquidGlassRadius.shapePill,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(LiquidGlassSpacing.sm))

            // Drawer header — subtle "N apps" label so the sheet feels like a
            // proper drawer surface rather than a bare grid.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "All Apps" else "Results",
                    style = LiquidGlassTypography.footnote,
                    color = drawerColors.textSecondary
                )
                Text(
                    text = "${filteredApps.size}",
                    style = LiquidGlassTypography.footnote,
                    color = drawerColors.textSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(LiquidGlassSpacing.xs))

            // App Grid
            val listState = rememberLazyGridState()

            // Nested scroll to detect overscroll at top
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    // We removed custom onPreScroll and onPostScroll logic because `swipeable`
                    // should handle drag gestures if configured correctly.
                    // The main issue was aggressive auto-closing on fling.

                    // We only want to handle FLING here.
                    // Drag is handled by the parent swipeable modifier if the child (LazyGrid)
                    // cannot scroll further up.
                    // However, LazyGrid consumes all drag events usually.

                    // To solve "scrolling up closes it": We must prevent the swipeable from seeing the drag
                    // UNLESS we are truly at the top and pulling down.

                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        // If the user is scrolling UP (available.y < 0) but the drawer is partially
                        // dragged down (offset > 0), we should consume the scroll to pull it back up.
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
                        // If the list has reached the top and there is still downward scroll available,
                        // feed it to the swipeable state to drag the drawer down.
                        if (available.y > 0 && source == NestedScrollSource.Drag) {
                            scope.launch {
                                swipeableState.performDrag(available.y)
                            }
                            return available
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        // If the drawer is partially dragged (offset > 0), we must consume the fling
                        // to ensure it settles to an anchor (Open or Closed).
                        if (swipeableState.offset.value > 0f) {
                            swipeableState.performFling(available.y)
                            return available
                        }
                        return Velocity.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        // We REMOVE the logic that auto-closes the drawer on rapid downward fling (available.y > 0).
                        // This was likely causing the "auto closes when scrolling to top" issue.
                        // When a user flings the list up to reach the top, the momentum might result in
                        // a positive 'available' velocity once it hits the top edge.
                        // By removing the animateTo(0) call here, the drawer stays open unless explicitly dragged.
                        return Velocity.Zero
                    }
                }
            }

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
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppDrawerItem(
                            app = app,
                            glassSettings = glassSettings,
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { appForOptions = app }
                        )
                    }
                }

                if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = drawerColors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(LiquidGlassSpacing.sm))
                        Text(
                            "No apps match \"$searchQuery\"",
                            style = LiquidGlassTypography.subheadline,
                            color = drawerColors.textSecondary
                        )
                    }
                }
            }
        }
    }

    // Options Dialog
    if (appForOptions != null && !showEditDialog) {
        AppOptionsDialog(
            app = appForOptions!!,
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
            }
        )
    }

    // Edit Dialog
    if (showEditDialog && appForOptions != null) {
        EditAppInfoDialog(
            app = appForOptions!!,
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

    // Subtle entrance so the grid doesn't just pop in — feels closer to a
    // native "premium" launcher drawer.
    var appeared by remember(app.packageName) { mutableStateOf(false) }
    LaunchedEffect(app.packageName) { appeared = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "appEntranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(180),
        label = "appEntranceAlpha"
    )

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
                scaleX = entranceScale * pressScale
                scaleY = entranceScale * pressScale
                alpha = entranceAlpha
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
