package dev.electrikjesus.xrlauncher.core.workspace

import android.util.Log
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.workspace.scene.DeskPhysics
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

/** Placed Desktop icons + live drag from All Apps / Home onto empty sphere space. */
object HomeSpaceDeskState {
    private const val DRAG_SLOP = 0.018f
    private const val ANGLE_SLOP_DEG = 3.5f
    private const val LOG_TAG = "XRLauncher/Desk"

    data class Drag(
        val app: HomeSpaceDesk.AppRef,
        /** True when pulled from the open All Apps drawer (not already on the desk). */
        val fromDrawer: Boolean,
        /**
         * True when pulled from the Home pane grid. Home stays unchanged — this only
         * places (or repositions) a Desktop copy.
         */
        val fromHome: Boolean,
        val startX: Float,
        val startY: Float,
        val startYawDeg: Float,
        val startPitchDeg: Float,
        val yawDeg: Float,
        val pitchDeg: Float,
        val pulling: Boolean,
    )

    private val _placed = MutableStateFlow<List<HomeSpaceDesk.Placed>>(emptyList())
    val placedFlow: StateFlow<List<HomeSpaceDesk.Placed>> = _placed.asStateFlow()
    val placed: List<HomeSpaceDesk.Placed> get() = _placed.value

    private val _drag = MutableStateFlow<Drag?>(null)
    val dragFlow: StateFlow<Drag?> = _drag.asStateFlow()
    val drag: Drag? get() = _drag.value

    /** Closed All Apps tile pose; null keeps the default left-of-Home yaw. */
    private val _drawerPose = MutableStateFlow<Pair<Float, Float>?>(null)
    val drawerPoseFlow: StateFlow<Pair<Float, Float>?> = _drawerPose.asStateFlow()
    val drawerPose: Pair<Float, Float>? get() = _drawerPose.value

    /** Pager / All Apps tile pressed under Hold-Left — fired on pointer-up if not dragging. */
    private var pendingChrome: HomeSpaceDesk.Icon? = null

    fun hasActiveGesture(): Boolean = _drag.value != null || pendingChrome != null

    /**
     * @param hitYawDeg / [hitPitchDeg] sphere angles under the cursor at press (not icon center).
     * Anchoring pull-slop here keeps off-center grabs and FPS look-follow from instantly pulling.
     * @param fromHome when true, dropping onto Desktop copies the app onto the desk without
     * removing it from the Home pane list.
     */
    fun press(
        icon: HomeSpaceDesk.Icon,
        cursorX: Float,
        cursorY: Float,
        hitYawDeg: Float = icon.yawDeg,
        hitPitchDeg: Float = icon.pitchDeg,
        fromHome: Boolean = false,
    ) {
        pendingChrome = when {
            icon.isPager || icon.isAppDrawer -> icon
            else -> null
        }
        if (icon.isPager || icon.isBacking) {
            _drag.value = null
            return
        }
        if (!icon.isDesktopApp && !icon.isAppDrawer && !icon.isWidget) {
            _drag.value = null
            return
        }
        _drag.value = Drag(
            app = icon.app,
            fromDrawer = !fromHome && icon.isDesktopApp && icon.lift > 0f,
            fromHome = fromHome,
            startX = cursorX,
            startY = cursorY,
            startYawDeg = hitYawDeg,
            startPitchDeg = hitPitchDeg,
            yawDeg = hitYawDeg,
            pitchDeg = hitPitchDeg,
            pulling = false,
        )
    }

    fun move(cursorX: Float, cursorY: Float, yawDeg: Float, pitchDeg: Float) {
        val current = _drag.value ?: return
        val pulled = current.pulling ||
            hypot(cursorX - current.startX, cursorY - current.startY) > DRAG_SLOP ||
            hypot(yawDeg - current.startYawDeg, pitchDeg - current.startPitchDeg) > ANGLE_SLOP_DEG
        if (pulled) pendingChrome = null
        _drag.value = current.copy(
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            pulling = pulled,
        )
    }

    /**
     * Call from companion pointer-up **before** clearing `isPressed`.
     * Handles pending pager/All-Apps chrome clicks and desk-icon pulls.
     * @return true when the companion must not emit a click/drag.
     */
    fun notePointerUp(cursorMoved: Boolean): Boolean {
        val chrome = pendingChrome
        pendingChrome = null
        if (chrome != null && _drag.value?.pulling != true) {
            // Drop the unused grab so All Apps/pager open isn't stuck in a live drag pose.
            _drag.value = null
            fireChrome(chrome)
            return true
        }
        val current = _drag.value ?: return false
        if (!current.pulling && !cursorMoved) return false
        if (!current.pulling) {
            _drag.value = current.copy(pulling = true)
        }
        return true
    }

    private fun fireChrome(icon: HomeSpaceDesk.Icon) {
        when (icon.kind) {
            HomeSpaceDesk.Kind.APP_DRAWER -> {
                Log.d(LOG_TAG, "chrome click all-apps tile")
                GlassesSessionState.toggleAllAppsOverlay()
            }
            HomeSpaceDesk.Kind.PAGE_PREV -> {
                Log.d(LOG_TAG, "chrome click page prev")
                AllAppsPaginationState.prevPage()
            }
            HomeSpaceDesk.Kind.PAGE_NEXT -> {
                Log.d(LOG_TAG, "chrome click page next")
                AllAppsPaginationState.nextPage()
            }
            HomeSpaceDesk.Kind.PAGE -> {
                HomeSpaceDesk.pageIndex(icon.componentKey)?.let {
                    Log.d(LOG_TAG, "chrome click page $it")
                    AllAppsPaginationState.goToPage(it)
                }
            }
            else -> Unit
        }
    }

    /**
     * @param obstacles desk icons that block placement (All Apps tile, open widget, other apps).
     * @param panes Home/Tray/app panes that reject drops.
     * @return true if a drag was consumed (no click).
     */
    fun release(
        onDesktop: Boolean,
        halfWidth: Float = HomeSpaceDesk.ICON_HALF_WIDTH,
        halfHeight: Float = HomeSpaceDesk.labeledIconHalfHeight(HomeSpaceDesk.ICON_HALF_WIDTH),
        sphereScale: Float = 1f,
        obstacles: List<HomeSpaceDesk.Icon> = emptyList(),
        panes: List<HomeSpaceScene.Pane> = emptyList(),
    ): Boolean {
        val current = _drag.value ?: return false
        _drag.value = null
        if (!current.pulling) return false
        if (current.app.componentKey == HomeSpaceDesk.DRAWER_KEY ||
            current.app.kind == HomeSpaceDesk.Kind.APP_DRAWER
        ) {
            if (!onDesktop) return true
            val resolved = HomeSpaceDesk.resolveDesktopDrop(
                yawDeg = current.yawDeg,
                pitchDeg = current.pitchDeg,
                halfWidth = halfWidth,
                halfHeight = halfHeight,
                sphereScale = sphereScale,
                obstacles = obstacles,
                paneBlocks = panes,
                excludeKey = current.app.componentKey,
            ) ?: return true
            _drawerPose.value = resolved.first to resolved.second
            return true
        }
        val wasOnDesktop = _placed.value.any { it.app.componentKey == current.app.componentKey }
        // BumpDesk: drag a Desktop icon onto the All Apps tile (or open backing) to remove it.
        // Widgets are not returned to All Apps — they stay until Delete from radial.
        if (
            onDesktop &&
            current.app.kind != HomeSpaceDesk.Kind.WIDGET &&
            HomeSpaceDesk.hitsAllAppsReturn(
                yawDeg = current.yawDeg,
                pitchDeg = current.pitchDeg,
                halfWidth = halfWidth,
                halfHeight = halfHeight,
                sphereScale = sphereScale,
                obstacles = obstacles,
            )
        ) {
            if (wasOnDesktop) {
                Log.d(LOG_TAG, "return to All Apps — remove ${current.app.label}")
                _placed.value = _placed.value.filter { it.app.componentKey != current.app.componentKey }
            } else {
                Log.d(LOG_TAG, "drop on All Apps — cancel place ${current.app.label}")
            }
            return true
        }
        if (!onDesktop) return true
        val existing = _placed.value.firstOrNull { it.app.componentKey == current.app.componentKey }
        val dropHalfW = existing?.halfWidth ?: halfWidth
        val dropHalfH = existing?.halfHeight ?: halfHeight
        val resolved = HomeSpaceDesk.resolveDesktopDrop(
            yawDeg = current.yawDeg,
            pitchDeg = current.pitchDeg,
            halfWidth = dropHalfW,
            halfHeight = dropHalfH,
            sphereScale = sphereScale,
            obstacles = obstacles,
            paneBlocks = panes,
            excludeKey = current.app.componentKey,
        ) ?: return true
        // Place at rest — release impulse caused icons to jump after a grab.
        val next = _placed.value.filter { it.app.componentKey != current.app.componentKey } +
            HomeSpaceDesk.Placed(
                app = current.app,
                yawDeg = resolved.first,
                pitchDeg = resolved.second,
                velYawDeg = 0f,
                velPitchDeg = 0f,
                halfWidth = existing?.halfWidth,
                halfHeight = existing?.halfHeight,
            )
        _placed.value = next
        return true
    }

    /**
     * Advance BumpDesk-style physics for placed icons.
     * [pinnedObstacles] are immovable (All Apps tile, open widget, panes as angular boxes).
     */
    fun tickPhysics(
        dtSec: Float,
        sphereScale: Float,
        uiScale: Float,
        viewportWidthPx: Float = 1920f,
        viewportHeightPx: Float = 1080f,
        panelScale: Float = WorkspaceAppearance.DEFAULT_PANEL_SCALE,
        density: Float = 2f,
        pinnedObstacles: List<HomeSpaceDesk.Icon>,
        panes: List<HomeSpaceScene.Pane>,
    ) {
        val halfW = HomeSpaceDesk.iconHalfWidth(
            uiScale = uiScale,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            panelScale = panelScale,
            sphereScale = sphereScale,
            density = density,
        )
        val labeledHalfH = HomeSpaceDesk.labeledIconHalfHeight(halfW)
        val bodies = ArrayList<DeskPhysics.Body>(_placed.value.size + pinnedObstacles.size + panes.size)
        _placed.value.forEach { item ->
            val itemHalfW = item.halfWidth ?: halfW
            val itemHalfH = item.halfHeight ?: labeledHalfH
            bodies += DeskPhysics.Body(
                key = item.app.componentKey,
                yawDeg = item.yawDeg,
                pitchDeg = item.pitchDeg,
                velYawDeg = item.velYawDeg,
                velPitchDeg = item.velPitchDeg,
                halfYawDeg = HomeSpaceDesk.angularHalfYaw(itemHalfW, sphereScale),
                halfPitchDeg = HomeSpaceDesk.angularHalfPitch(itemHalfH, sphereScale),
                mass = DeskPhysics.massFor(itemHalfW, itemHalfH),
                pinned = false,
            )
        }
        pinnedObstacles.forEach { icon ->
            bodies += DeskPhysics.Body(
                key = icon.componentKey,
                yawDeg = icon.yawDeg,
                pitchDeg = icon.pitchDeg,
                velYawDeg = 0f,
                velPitchDeg = 0f,
                halfYawDeg = HomeSpaceDesk.angularHalfYaw(icon.halfWidth, sphereScale),
                halfPitchDeg = HomeSpaceDesk.angularHalfPitch(icon.halfHeight, sphereScale),
                mass = 100f,
                pinned = true,
            )
        }
        panes.forEach { pane ->
            bodies += DeskPhysics.Body(
                key = "pane:${pane.worldX}",
                yawDeg = pane.yawDeg,
                pitchDeg = 0f,
                velYawDeg = 0f,
                velPitchDeg = 0f,
                halfYawDeg = pane.halfWidthDeg,
                halfPitchDeg = pane.halfHeightDeg,
                mass = 100f,
                pinned = true,
            )
        }
        val draggingKey = _drag.value?.takeIf { it.pulling }?.app?.componentKey
        if (draggingKey != null) {
            val drag = _drag.value!!
            val existing = _placed.value.firstOrNull { it.app.componentKey == draggingKey }
            val dragHalfW = existing?.halfWidth ?: halfW
            val dragHalfH = existing?.halfHeight ?: labeledHalfH
            bodies += DeskPhysics.Body(
                key = draggingKey,
                yawDeg = drag.yawDeg,
                pitchDeg = drag.pitchDeg,
                velYawDeg = 0f,
                velPitchDeg = 0f,
                halfYawDeg = HomeSpaceDesk.angularHalfYaw(dragHalfW, sphereScale),
                halfPitchDeg = HomeSpaceDesk.angularHalfPitch(dragHalfH, sphereScale),
                mass = DeskPhysics.massFor(dragHalfW, dragHalfH),
                pinned = false,
            )
        }
        DeskPhysics.step(bodies, dtSec, manipulatedKey = draggingKey)
        val byKey = bodies.associateBy { it.key }
        var changed = false
        val updated = _placed.value.map { item ->
            val body = byKey[item.app.componentKey] ?: return@map item
            if (body.yawDeg != item.yawDeg || body.pitchDeg != item.pitchDeg ||
                body.velYawDeg != item.velYawDeg || body.velPitchDeg != item.velPitchDeg
            ) {
                changed = true
                item.copy(
                    yawDeg = body.yawDeg,
                    pitchDeg = body.pitchDeg,
                    velYawDeg = body.velYawDeg,
                    velPitchDeg = body.velPitchDeg,
                )
            } else {
                item
            }
        }
        if (changed) _placed.value = updated
    }

    fun cancel() {
        _drag.value = null
        pendingChrome = null
    }

    fun clear() {
        _placed.value = emptyList()
        _drag.value = null
        _drawerPose.value = null
        pendingChrome = null
    }

    fun restore(layout: DeskLayout) {
        _placed.value = layout.items.map { item ->
            val kind = when {
                item.kind == "WIDGET" || item.componentKey.startsWith("widget_") ->
                    HomeSpaceDesk.Kind.WIDGET
                else -> HomeSpaceDesk.Kind.APP
            }
            HomeSpaceDesk.Placed(
                app = HomeSpaceDesk.AppRef(
                    componentKey = item.componentKey,
                    label = item.label,
                    packageName = item.packageName,
                    kind = kind,
                ),
                yawDeg = item.yawDeg,
                pitchDeg = item.pitchDeg,
                velYawDeg = 0f,
                velPitchDeg = 0f,
                halfWidth = item.halfWidth,
                halfHeight = item.halfHeight,
            )
        }
        _drawerPose.value = layout.drawerYawDeg?.let { yaw ->
            yaw to (layout.drawerPitchDeg ?: 0f)
        }
        _drag.value = null
        pendingChrome = null
    }

    fun toLayout(): DeskLayout = DeskLayout(
        items = _placed.value.map { item ->
            DeskPlacedItem(
                componentKey = item.app.componentKey,
                label = item.app.label,
                packageName = item.app.packageName,
                yawDeg = item.yawDeg,
                pitchDeg = item.pitchDeg,
                kind = if (item.app.kind == HomeSpaceDesk.Kind.WIDGET) "WIDGET" else "APP",
                halfWidth = item.halfWidth,
                halfHeight = item.halfHeight,
            )
        },
        drawerYawDeg = _drawerPose.value?.first,
        drawerPitchDeg = _drawerPose.value?.second,
    )

    /** Drop icons whose apps are no longer installed. Widgets are kept (host id is durable). */
    fun pruneMissing(validComponentKeys: Set<String>): Boolean {
        val next = _placed.value.filter {
            it.app.kind == HomeSpaceDesk.Kind.WIDGET || it.app.componentKey in validComponentKeys
        }
        if (next.size == _placed.value.size) return false
        _placed.value = next
        return true
    }

    /** Remove placed Desktop icons / widgets by component key (lasso / radial selection). */
    fun removeByKeys(componentKeys: Set<String>): Boolean {
        if (componentKeys.isEmpty()) return false
        val removed = _placed.value.filter { it.app.componentKey in componentKeys }
        if (removed.isEmpty()) return false
        removed.forEach { item ->
            if (item.app.kind == HomeSpaceDesk.Kind.WIDGET) {
                DeskWidgetUtils.parseWidgetId(item.app.componentKey)?.let { id ->
                    DeskWidgetController.deleteWidget(id)
                }
            }
        }
        _placed.value = _placed.value.filter { it.app.componentKey !in componentKeys }
        return true
    }

    /** Place a live AppWidgetHost face on the sphere (BumpDesk addWidgetAt). */
    fun placeWidget(
        appWidgetId: Int,
        label: String,
        packageName: String,
        yawDeg: Float,
        pitchDeg: Float,
        halfWidth: Float,
        halfHeight: Float,
    ) {
        val key = DeskWidgetUtils.widgetKey(appWidgetId)
        val next = _placed.value.filter { it.app.componentKey != key } +
            HomeSpaceDesk.Placed(
                app = HomeSpaceDesk.AppRef(
                    componentKey = key,
                    label = label,
                    packageName = packageName,
                    kind = HomeSpaceDesk.Kind.WIDGET,
                ),
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                halfWidth = halfWidth,
                halfHeight = halfHeight,
            )
        _placed.value = next
    }

    /**
     * BumpDesk lasso layout: rearrange selected Desktop icons (row / column / grid / stack / folder).
     */
    fun arrangeSelected(
        keys: Set<String>,
        mode: DeskArrangeMode,
        sphereScale: Float,
        halfWidth: Float,
        halfHeight: Float,
    ): Boolean {
        val next = DeskArrange.arrange(
            placed = _placed.value,
            keys = keys,
            mode = mode,
            sphereScale = sphereScale,
            halfWidth = halfWidth,
            halfHeight = halfHeight,
        ) ?: return false
        _placed.value = next
        return true
    }
}
