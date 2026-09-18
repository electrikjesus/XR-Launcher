package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.GlassesAppPlane
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeSpace3d
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceScaledLayer
import dev.electrikjesus.xrlauncher.ui.workspace.AppIconCell
import dev.electrikjesus.xrlauncher.ui.workspace.ClockWidgetPanel
import dev.electrikjesus.xrlauncher.ui.workspace.PaginatedAppGrid

private val PillBg = Color(0xCC1C1C1E)
private val CardBg = Color(0xE61C1C1E)
private val Accent = Color(0xFF8AB4F8)

@Composable
fun GlassesHomeSpace(
    launchableApps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    hoveredLabel: String?,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val homeApps = remember(launchableApps, hotseatApps) {
        (hotseatApps + launchableApps).distinctBy { it.packageName }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassesHomeClock()
        GlassesHomeActionPills(
            hoveredLabel = hoveredLabel,
            onBoundsChanged = onBoundsChanged,
            onHome = { GlassesHomeLook.lookAt(GlassesHomeLook.PANE_HOME) },
            onAllApps = onOpenAllApps,
            onRecents = onOpenRecents,
            onNotifications = onOpenNotifications,
            onQuickSettings = onOpenQuickSettings,
            onSettings = onOpenSettings,
            modifier = Modifier.padding(top = 16.dp),
        )
        PaginatedAppGrid(
            apps = homeApps,
            pageIndex = pageIndex,
            onPageChange = onPageChange,
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
            onAppContextMenu = onAppContextMenu,
            columns = 5,
            rows = 2,
            showPageControls = true,
            iconSize = 92.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
fun GlassesXrAllAppsLayer(
    apps: List<LaunchableApp>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onDismiss: () -> Unit,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassesHomeClock()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            GlassesCircleButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.all_apps_close),
                boundsKey = GlassesHomeHits.OVERLAY_CLOSE,
                hovered = hoveredLabel == GlassesHomeHits.CLOSE_LABEL,
                onBoundsChanged = onBoundsChanged,
                onClick = onDismiss,
            )
        }
        PaginatedAppGrid(
            apps = apps,
            pageIndex = pageIndex,
            onPageChange = onPageChange,
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
            onAppContextMenu = onAppContextMenu,
            columns = 4,
            rows = 3,
            showPageControls = true,
            iconSize = 88.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
fun GlassesRecentsLayer(
    recents: List<LaunchableApp>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
            .then(if (dismissOnScrim) Modifier.clickable(onClick = onDismiss) else Modifier),
    ) {
        if (recents.isEmpty()) {
            Text(
                text = stringResource(R.string.xr_recents_empty),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                recents.take(3).forEachIndexed { index, app ->
                    GlassesRecentCard(
                        app = app,
                        hovered = hoveredLabel == app.label,
                        lift = if (index == 1) 24.dp else 0.dp,
                        onBoundsChanged = onBoundsChanged,
                        onLaunch = { onLaunchApp(app) },
                    )
                }
            }
        }
        GlassesCapsuleButton(
            label = stringResource(R.string.xr_clear_all),
            boundsKey = GlassesHomeHits.RECENTS_CLEAR,
            hovered = hoveredLabel == GlassesHomeHits.CLEAR_ALL_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onClear,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        )
    }
}

@Composable
fun GlassesNotificationsLayer(
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (dismissOnScrim) {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.12f))
                        .clickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.55f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassesNotificationCard(
                title = stringResource(R.string.xr_notifications_empty_title),
                body = stringResource(R.string.xr_notifications_empty_body),
            )
        }
        GlassesCapsuleButton(
            label = stringResource(R.string.xr_clear_all),
            boundsKey = GlassesHomeHits.NOTIFICATIONS_CLEAR,
            hovered = hoveredLabel == GlassesHomeHits.CLEAR_ALL_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onClear,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 64.dp),
        )
    }
}

@Composable
fun GlassesQuickSettingsLayer(
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    var brightness by remember { mutableFloatStateOf(0.65f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (dismissOnScrim) {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.22f))
                        .clickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(CardBg)
                .clickable(onClick = {})
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassesQsPill(
                    icon = Icons.Default.Star,
                    label = stringResource(R.string.xr_qs_internet),
                    modifier = Modifier.weight(1f),
                )
                GlassesQsPill(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.xr_qs_bluetooth),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                GlassesQsTile(Icons.Default.Star, stringResource(R.string.xr_qs_dark_theme))
                GlassesQsTile(Icons.Default.Notifications, stringResource(R.string.xr_notifications))
                GlassesQsTile(Icons.Default.Settings, stringResource(R.string.settings_title), onClick = onOpenSettings)
                GlassesQsTile(Icons.Default.Menu, stringResource(R.string.xr_qs_brightness))
            }
            Slider(value = brightness, onValueChange = { brightness = it })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                GlassesCircleButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.xr_qs_power),
                    boundsKey = GlassesHomeHits.OVERLAY_CLOSE,
                    hovered = hoveredLabel == GlassesHomeHits.CLOSE_LABEL,
                    onBoundsChanged = onBoundsChanged,
                    onClick = onDismiss,
                    diameter = 44.dp,
                )
                GlassesCircleButton(
                    icon = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_open),
                    boundsKey = GlassesHomeHits.SETTINGS,
                    hovered = hoveredLabel == GlassesHomeHits.SETTINGS_LABEL,
                    onBoundsChanged = onBoundsChanged,
                    onClick = onOpenSettings,
                    diameter = 44.dp,
                )
            }
        }
    }
}

@Composable
private fun GlassesHomeActionPills(
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onHome: () -> Unit,
    onAllApps: () -> Unit,
    onRecents: () -> Unit,
    onNotifications: () -> Unit,
    onQuickSettings: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassesCircleButton(
            icon = Icons.Default.Home,
            contentDescription = stringResource(R.string.xr_home),
            boundsKey = GlassesHomeHits.HOME,
            hovered = hoveredLabel == GlassesHomeHits.HOME_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onHome,
        )
        GlassesCircleButton(
            icon = Icons.Default.Apps,
            contentDescription = stringResource(R.string.all_apps),
            boundsKey = GlassesHomeHits.ALL_APPS,
            hovered = hoveredLabel == GlassesHomeHits.ALL_APPS_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onAllApps,
        )
        GlassesCircleButton(
            icon = Icons.Default.Menu,
            contentDescription = stringResource(R.string.xr_recents),
            boundsKey = GlassesHomeHits.RECENTS,
            hovered = hoveredLabel == GlassesHomeHits.RECENTS_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onRecents,
        )
        GlassesCircleButton(
            icon = Icons.Default.Notifications,
            contentDescription = stringResource(R.string.xr_notifications),
            boundsKey = GlassesHomeHits.NOTIFICATIONS,
            hovered = hoveredLabel == GlassesHomeHits.NOTIFICATIONS_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onNotifications,
        )
        GlassesCircleButton(
            icon = Icons.Default.Star,
            contentDescription = stringResource(R.string.xr_quick_settings),
            boundsKey = GlassesHomeHits.QUICK_SETTINGS,
            hovered = hoveredLabel == GlassesHomeHits.QUICK_SETTINGS_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onQuickSettings,
        )
        GlassesCircleButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings_open),
            boundsKey = GlassesHomeHits.SETTINGS,
            hovered = hoveredLabel == GlassesHomeHits.SETTINGS_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = onSettings,
        )
    }
}

@Composable
private fun GlassesCircleButton(
    icon: ImageVector,
    contentDescription: String,
    boundsKey: String,
    hovered: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
    diameter: Dp = 80.dp,
) {
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(if (hovered) Accent.copy(alpha = 0.55f) else PillBg)
            .clickable(onClick = onClick)
            .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(diameter * 0.45f),
        )
    }
}

@Composable
private fun GlassesCapsuleButton(
    label: String,
    boundsKey: String,
    hovered: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (hovered) Accent.copy(alpha = 0.55f) else PillBg)
            .clickable(onClick = onClick)
            .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun GlassesRecentCard(
    app: LaunchableApp,
    hovered: Boolean,
    lift: Dp,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .offset(y = -lift)
            .width(220.dp)
            .clickable(onClick = onLaunch)
            .onGloballyPositioned { onBoundsChanged(app.componentName.flattenToString(), it.boundsInRoot()) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PillBg)
                .padding(8.dp),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (hovered) Color(0xFF2C2C2E) else Color(0xFF111111)),
            contentAlignment = Alignment.Center,
        ) {
            AppIconCell(
                app = app,
                isHovered = hovered,
                iconSize = 88.dp,
                onBoundsChanged = onBoundsChanged,
                onLaunchApp = { onLaunch() },
            )
        }
    }
}

@Composable
private fun GlassesNotificationCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(18.dp),
    ) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(
            text = body,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GlassesQsPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun GlassesQsTile(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF3A3A3C))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun GlassesHomeCarousel(
    panNorm: Float,
    cursorX: Float,
    cursorY: Float,
    appPlanes: List<GlassesAppPlane>,
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable () -> Unit,
    appPane: @Composable (GlassesAppPlane) -> Unit,
    modifier: Modifier = Modifier,
    uiScale: Float = WorkspaceAppearance.DEFAULT_UI_SCALE,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        data class Slot(val key: String, val worldX: Float, val content: @Composable () -> Unit)
        val slots = buildList {
            add(Slot("all_apps", GlassesHomeLook.PANE_LEFT, left))
            add(Slot("home", GlassesHomeLook.PANE_HOME, center))
            appPlanes.forEachIndexed { index, plane ->
                add(Slot(plane.panelId, GlassesHomeLook.appPane(index)) { appPane(plane) })
            }
            add(Slot("tray", GlassesHomeLook.trayPane(), right))
        }
        slots
            .map { slot ->
                slot to GlassesHomeSpace3d.projectPane(
                    worldX = slot.worldX,
                    look = panNorm,
                    cursorX = cursorX,
                    cursorY = cursorY,
                    viewportWidthPx = widthPx,
                    viewportHeightPx = heightPx,
                )
            }
            .filter { it.second.visible }
            .sortedBy { it.second.viewZ }
            .forEach { (slot, projected) ->
                androidx.compose.runtime.key(slot.key) {
                    CarouselPane(projected = projected, uiScale = uiScale, content = slot.content)
                }
            }
    }
}

@Composable
private fun CarouselPane(
    projected: GlassesHomeSpace3d.ProjectedPane,
    uiScale: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(GlassesHomeSpace3d.PANE_WIDTH_FRACTION)
                .fillMaxHeight(GlassesHomeSpace3d.PANE_HEIGHT_FRACTION)
                .graphicsLayer {
                    translationX = projected.translationXPx
                    translationY = projected.translationYPx
                    rotationY = projected.rotationYDeg
                    rotationX = projected.rotationXDeg
                    scaleX = projected.scale
                    scaleY = projected.scale
                    alpha = projected.alpha
                    cameraDistance = projected.cameraDistancePx
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .clipToBounds(),
        ) {
            WorkspaceScaledLayer(uiScale = uiScale) {
                content()
            }
        }
    }
}

@Composable
fun GlassesAppPlaneLayer(
    plane: GlassesAppPlane,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(28.dp))
                .background(CardBg)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                GlassesCircleButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.all_apps_close),
                    boundsKey = GlassesHomeHits.appCloseKey(plane.panelId),
                    hovered = hoveredLabel == GlassesHomeHits.CLOSE_LABEL,
                    onBoundsChanged = onBoundsChanged,
                    onClick = onClose,
                    diameter = 44.dp,
                )
            }
            Text(
                text = plane.label,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.xr_spatial_window_body, plane.label),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun GlassesHomeTrayPane(
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GlassesNotificationsLayer(
            hoveredLabel = hoveredLabel,
            onBoundsChanged = onBoundsChanged,
            onClear = {},
            onDismiss = { GlassesHomeLook.lookHome() },
            dismissOnScrim = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        GlassesQuickSettingsLayer(
            hoveredLabel = hoveredLabel,
            onBoundsChanged = onBoundsChanged,
            onOpenSettings = onOpenSettings,
            onDismiss = { GlassesHomeLook.lookHome() },
            dismissOnScrim = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun GlassesHomeClock() {
    ClockWidgetPanel(centered = true)
}
