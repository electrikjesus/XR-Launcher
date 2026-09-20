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

    private val _piles = MutableStateFlow<List<DeskPile>>(emptyList())
    val pilesFlow: StateFlow<List<DeskPile>> = _piles.asStateFlow()
    val piles: List<DeskPile> get() = _piles.value

    /** Last tickPhysics context — used by breakPile so released icons clear panes. */
    private var lastSphereScale: Float = 1f
    private var lastIconHalfW: Float = HomeSpaceDesk.ICON_HALF_WIDTH
    private var lastPanes: List<HomeSpaceScene.Pane> = emptyList()
    private var lastPinnedObstacles: List<HomeSpaceDesk.Icon> = emptyList()

    private val _drag = MutableStateFlow<Drag?>(null)
    val dragFlow: StateFlow<Drag?> = _drag.asStateFlow()
    val drag: Drag? get() = _drag.value

    /** Closed All Apps tile pose; null keeps the default left-of-Home yaw. */
    private val _drawerPose = MutableStateFlow<Pair<Float, Float>?>(null)
    val drawerPoseFlow: StateFlow<Pair<Float, Float>?> = _drawerPose.asStateFlow()
    val drawerPose: Pair<Float, Float>? get() = _drawerPose.value

    /** Pager / All Apps tile pressed under Hold-Left — fired on pointer-up if not dragging. */
    private var pendingChrome: HomeSpaceDesk.Icon? = null

    fun hasActiveGesture(): Boolean =
        _drag.value != null ||
            pendingChrome != null ||
            DeskGroupMoveState.isDragging ||
            DeskWidgetResizeState.isDragging

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
        if (icon.isPager || icon.isBacking || icon.isPileBacking) {
            _drag.value = null
            return
        }
        if (!icon.isDesktopApp && !icon.isAppDrawer && !icon.isWidget && !icon.isPileFace) {
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
        if (DeskGroupMoveState.isDragging) {
            // Group end is finalized in trackDeskDrag; consume companion click after a pull.
            return DeskGroupMoveState.isPulling || cursorMoved
        }
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
            HomeSpaceDesk.Kind.GROUP_HANDLE,
            HomeSpaceDesk.Kind.RESIZE_HANDLE,
            HomeSpaceDesk.Kind.APP,
            HomeSpaceDesk.Kind.WIDGET,
            HomeSpaceDesk.Kind.DRAWER_BACKING,
            HomeSpaceDesk.Kind.PILE_STACK,
            HomeSpaceDesk.Kind.PILE_FOLDER,
            HomeSpaceDesk.Kind.PILE_BACKING,
            -> Unit
        }
    }

    /** Apply yaw/pitch poses for a rigid group move (relative arrangement preserved). */
    fun applyGroupPoses(poses: Map<String, Pair<Float, Float>>) {
        if (poses.isEmpty()) return
        val grid = DeskGridOverlay.config
        var changed = false
        val next = _placed.value.map { item ->
            val pose = poses[item.app.componentKey] ?: return@map item
            var yaw = pose.first
            var pitch = pose.second
            if (grid.snapToGrid) {
                val snapped = DeskGrid.snapPose(
                    yawDeg = yaw,
                    pitchDeg = pitch,
                    iconHalfWidth = grid.iconHalfWidth,
                    gridScale = grid.gridScale,
                    sphereScale = grid.sphereScale,
                )
                yaw = snapped.first
                pitch = snapped.second
            }
            if (item.yawDeg == yaw && item.pitchDeg == pitch) return@map item
            changed = true
            item.copy(
                yawDeg = yaw,
                pitchDeg = pitch,
                velYawDeg = 0f,
                velPitchDeg = 0f,
            )
        }
        if (changed) _placed.value = next
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
        val wasPile = _piles.value.any { it.id == current.app.componentKey }
        // BumpDesk: drag a Desktop icon onto the All Apps tile (or open backing) to remove it.
        // Widgets are not returned to All Apps — they stay until Delete from radial.
        // Piles break apart onto the desk when dropped on All Apps.
        if (
            onDesktop &&
            current.app.kind != HomeSpaceDesk.Kind.WIDGET &&
            !current.app.kind.name.startsWith("PILE_") &&
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
        if (wasPile ||
            current.app.kind == HomeSpaceDesk.Kind.PILE_STACK ||
            current.app.kind == HomeSpaceDesk.Kind.PILE_FOLDER
        ) {
            val resolved = HomeSpaceDesk.resolveDesktopDrop(
                yawDeg = current.yawDeg,
                pitchDeg = current.pitchDeg,
                halfWidth = halfWidth,
                halfHeight = halfHeight,
                sphereScale = sphereScale,
                obstacles = obstacles.filter { !DeskPile.isPileKey(it.componentKey) },
                paneBlocks = panes,
                excludeKey = current.app.componentKey,
            ) ?: return true
            var placeYaw = resolved.first
            var placePitch = resolved.second
            val grid = DeskGridOverlay.config
            if (grid.snapToGrid) {
                val snapped = DeskGrid.snapPose(
                    yawDeg = placeYaw,
                    pitchDeg = placePitch,
                    iconHalfWidth = grid.iconHalfWidth,
                    gridScale = grid.gridScale,
                    sphereScale = sphereScale,
                )
                placeYaw = snapped.first
                placePitch = snapped.second
            }
            _piles.value = _piles.value.map { pile ->
                if (pile.id == current.app.componentKey) {
                    pile.copy(yawDeg = placeYaw, pitchDeg = placePitch)
                } else {
                    pile
                }
            }
            return true
        }
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
        var placeYaw = resolved.first
        var placePitch = resolved.second
        val grid = DeskGridOverlay.config
        if (grid.snapToGrid) {
            val snapped = DeskGrid.snapPose(
                yawDeg = placeYaw,
                pitchDeg = placePitch,
                iconHalfWidth = grid.iconHalfWidth,
                gridScale = grid.gridScale,
                sphereScale = sphereScale,
            )
            placeYaw = snapped.first
            placePitch = snapped.second
        }
        // Place at rest — release impulse caused icons to jump after a grab.
        val next = _placed.value.filter { it.app.componentKey != current.app.componentKey } +
            HomeSpaceDesk.Placed(
                app = current.app,
                yawDeg = placeYaw,
                pitchDeg = placePitch,
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
        lastSphereScale = sphereScale
        lastIconHalfW = halfW
        lastPanes = panes
        lastPinnedObstacles = pinnedObstacles
        val groupKeys = DeskGroupMoveState.armedKeys.takeIf { DeskGroupMoveState.isDragging }.orEmpty()
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
                pinned = item.app.componentKey in groupKeys,
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
        DeskGroupMoveState.cancelDrag()
        DeskWidgetResizeState.cancelDrag()
    }

    fun clear() {
        _placed.value = emptyList()
        _piles.value = emptyList()
        _drag.value = null
        _drawerPose.value = null
        pendingChrome = null
        lastSphereScale = 1f
        lastIconHalfW = HomeSpaceDesk.ICON_HALF_WIDTH
        lastPanes = emptyList()
        lastPinnedObstacles = emptyList()
        DeskGroupMoveState.clear()
        DeskWidgetResizeState.clear()
        DeskGridOverlay.hide()
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
        _piles.value = layout.piles.map { pile ->
            DeskPile(
                id = pile.id,
                mode = if (pile.mode == "FOLDER") DeskPileMode.FOLDER else DeskPileMode.STACK,
                name = pile.name,
                members = pile.members.map { m ->
                    HomeSpaceDesk.AppRef(
                        componentKey = m.componentKey,
                        label = m.label,
                        packageName = m.packageName,
                        kind = HomeSpaceDesk.Kind.APP,
                    )
                },
                yawDeg = pile.yawDeg,
                pitchDeg = pile.pitchDeg,
                expanded = false,
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
        piles = _piles.value.map { pile ->
            DeskPileItem(
                id = pile.id,
                name = pile.name,
                mode = pile.mode.name,
                yawDeg = pile.yawDeg,
                pitchDeg = pile.pitchDeg,
                members = pile.members.map { m ->
                    DeskPileMemberItem(
                        componentKey = m.componentKey,
                        label = m.label,
                        packageName = m.packageName,
                    )
                },
            )
        },
        drawerYawDeg = _drawerPose.value?.first,
        drawerPitchDeg = _drawerPose.value?.second,
    )

    /** Drop icons whose apps are no longer installed. Widgets are kept (host id is durable). */
    fun pruneMissing(validComponentKeys: Set<String>): Boolean {
        val nextPlaced = _placed.value.filter {
            it.app.kind == HomeSpaceDesk.Kind.WIDGET || it.app.componentKey in validComponentKeys
        }
        val singles = DeskPileOps.singletonReleases(_piles.value, validComponentKeys)
        val nextPiles = DeskPileOps.pruneMembers(_piles.value, validComponentKeys)
        val changed = nextPlaced.size != _placed.value.size ||
            nextPiles.size != _piles.value.size ||
            singles.isNotEmpty() ||
            nextPiles.zip(_piles.value).any { (a, b) -> a.members.size != b.members.size }
        if (!changed) return false
        _placed.value = nextPlaced + singles
        _piles.value = nextPiles
        return true
    }

    /** Remove placed Desktop icons / widgets by component key (lasso / radial selection). */
    fun removeByKeys(componentKeys: Set<String>): Boolean {
        if (componentKeys.isEmpty()) return false
        val removed = _placed.value.filter { it.app.componentKey in componentKeys }
        val removedPiles = _piles.value.filter { it.id in componentKeys }
        if (removed.isEmpty() && removedPiles.isEmpty()) return false
        removed.forEach { item ->
            if (item.app.kind == HomeSpaceDesk.Kind.WIDGET) {
                DeskWidgetUtils.parseWidgetId(item.app.componentKey)?.let { id ->
                    DeskWidgetController.deleteWidget(id)
                }
            }
        }
        _placed.value = _placed.value.filter { it.app.componentKey !in componentKeys }
        _piles.value = _piles.value.filter { it.id !in componentKeys }
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
     * Apply live widget resize from a corner drag (independent width/height).
     */
    fun applyWidgetResize(
        key: String,
        yawDeg: Float,
        pitchDeg: Float,
        halfWidth: Float,
        halfHeight: Float,
        recapture: Boolean = false,
    ): Boolean {
        var changed = false
        val next = _placed.value.map { item ->
            if (item.app.componentKey != key || item.app.kind != HomeSpaceDesk.Kind.WIDGET) {
                return@map item
            }
            val clamped = DeskWidgetUtils.clampHalfExtents(halfWidth, halfHeight)
            if (item.yawDeg == yawDeg && item.pitchDeg == pitchDeg &&
                item.halfWidth == clamped.first && item.halfHeight == clamped.second
            ) {
                return@map item
            }
            changed = true
            item.copy(
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                halfWidth = clamped.first,
                halfHeight = clamped.second,
                velYawDeg = 0f,
                velPitchDeg = 0f,
            )
        }
        if (!changed) return false
        _placed.value = next
        if (recapture) {
            DeskWidgetUtils.parseWidgetId(key)?.let { DeskWidgetController.requestRecapture(it) }
        }
        return true
    }

    /** BumpDesk createPileFromCaptured — STACK or FOLDER group at the selection centroid. */
    fun createPile(keys: Set<String>, mode: DeskPileMode): Boolean {
        val created = DeskPileOps.create(_placed.value, keys, mode) ?: return false
        _placed.value = created.second
        _piles.value = _piles.value + created.first
        DeskGroupMoveState.clear()
        Log.d(LOG_TAG, "createPile mode=$mode members=${created.first.members.size} id=${created.first.id}")
        return true
    }

    fun togglePileExpanded(pileId: String): Boolean {
        val pile = _piles.value.firstOrNull { it.id == pileId } ?: return false
        _piles.value = _piles.value.map {
            when {
                it.id == pileId -> if (pile.showsMembers) {
                    DeskPileOps.collapse(it)
                } else {
                    it.copy(expanded = true, fannedOut = false)
                }
                // Only one open pile at a time (BumpDesk collapseNonPinnedPiles).
                it.showsMembers -> DeskPileOps.collapse(it)
                else -> it
            }
        }
        return true
    }

    fun togglePileFan(pileId: String): Boolean {
        val pile = _piles.value.firstOrNull { it.id == pileId } ?: return false
        _piles.value = _piles.value.map {
            when {
                it.id == pileId -> DeskPileOps.toggleFan(it)
                it.showsMembers -> DeskPileOps.collapse(it)
                else -> it
            }
        }
        return true
    }

    fun breakPile(pileId: String): Boolean {
        val pile = _piles.value.firstOrNull { it.id == pileId } ?: return false
        _piles.value = _piles.value.filter { it.id != pileId }
        val halfW = lastIconHalfW
        val sphereScale = lastSphereScale
        val yawStep = DeskPileOps.breakApartYawStepDeg(
            sphereScale = sphereScale,
            halfWidth = halfW,
            memberCount = pile.members.size,
        )
        val releasedKeys = pile.members.map { it.componentKey }.toSet()
        var next = DeskPileOps.breakApart(pile, _placed.value, yawStepDeg = yawStep)
        // Static separation against desk icons / panes / drawer — no velocity impulses.
        next = settleBrokenApart(next, releasedKeys, halfW, sphereScale)
        _placed.value = next
        return true
    }

    /**
     * Push freshly released pile members off overlaps without imparting velocity.
     * Prevents DeskPhysics from ratcheting a deeply nested icon around the sphere.
     */
    private fun settleBrokenApart(
        placed: List<HomeSpaceDesk.Placed>,
        releasedKeys: Set<String>,
        halfW: Float,
        sphereScale: Float,
    ): List<HomeSpaceDesk.Placed> {
        val labeledHalfH = HomeSpaceDesk.labeledIconHalfHeight(halfW)
        val mutable = placed.toMutableList()
        val panes = lastPanes
        val pinned = lastPinnedObstacles
        repeat(10) {
            var moved = false
            for (i in mutable.indices) {
                val item = mutable[i]
                if (item.app.componentKey !in releasedKeys) continue
                val itemHalfW = item.halfWidth ?: halfW
                val itemHalfH = item.halfHeight ?: labeledHalfH
                val halfYaw = HomeSpaceDesk.angularHalfYaw(itemHalfW, sphereScale)
                val halfPitch = HomeSpaceDesk.angularHalfPitch(itemHalfH, sphereScale)
                var yaw = item.yawDeg
                var pitch = item.pitchDeg

                // Clear Home / Tray / app panes (large AABBs that otherwise orbit icons).
                panes.forEach { pane ->
                    if (yaw !in pane.yawMin..pane.yawMax || pitch !in pane.pitchMin..pane.pitchMax) {
                        return@forEach
                    }
                    val toMin = kotlin.math.abs(yaw - pane.yawMin)
                    val toMax = kotlin.math.abs(pane.yawMax - yaw)
                    yaw = if (toMin <= toMax) {
                        pane.yawMin - halfYaw - 0.75f
                    } else {
                        pane.yawMax + halfYaw + 0.75f
                    }
                    moved = true
                }

                // Clear All Apps tile / open drawer / other pinned blockers.
                pinned.forEach { other ->
                    val oHalfYaw = HomeSpaceDesk.angularHalfYaw(other.halfWidth, sphereScale)
                    val oHalfPitch = HomeSpaceDesk.angularHalfPitch(other.halfHeight, sphereScale)
                    if (!HomeSpaceDesk.overlapsAngular(
                            yaw, pitch, halfYaw, halfPitch,
                            other.yawDeg, other.pitchDeg, oHalfYaw, oHalfPitch,
                        )
                    ) {
                        return@forEach
                    }
                    val dy = DeskPhysics.shortestYawDelta(yaw, other.yawDeg)
                    val dp = pitch - other.pitchDeg
                    val needY = halfYaw + oHalfYaw + 0.75f
                    val needP = halfPitch + oHalfPitch + 0.75f
                    if (kotlin.math.abs(dy) * needP >= kotlin.math.abs(dp) * needY) {
                        yaw = other.yawDeg + needY * when {
                            dy > 0f -> 1f
                            dy < 0f -> -1f
                            else -> 1f
                        }
                    } else {
                        pitch = (other.pitchDeg + needP * if (dp >= 0f) 1f else -1f)
                            .coerceIn(-55f, 55f)
                    }
                    moved = true
                }

                mutable.forEachIndexed { j, other ->
                    if (i == j) return@forEachIndexed
                    val oHalfYaw = HomeSpaceDesk.angularHalfYaw(other.halfWidth ?: halfW, sphereScale)
                    val oHalfPitch = HomeSpaceDesk.angularHalfPitch(
                        other.halfHeight ?: labeledHalfH,
                        sphereScale,
                    )
                    if (!HomeSpaceDesk.overlapsAngular(
                            yaw, pitch, halfYaw, halfPitch,
                            other.yawDeg, other.pitchDeg, oHalfYaw, oHalfPitch,
                        )
                    ) {
                        return@forEachIndexed
                    }
                    val dy = DeskPhysics.shortestYawDelta(yaw, other.yawDeg)
                    val dp = pitch - other.pitchDeg
                    val needY = halfYaw + oHalfYaw + 0.5f
                    val needP = halfPitch + oHalfPitch + 0.5f
                    if (kotlin.math.abs(dy) * needP >= kotlin.math.abs(dp) * needY) {
                        yaw = other.yawDeg + needY * when {
                            dy > 0f -> 1f
                            dy < 0f -> -1f
                            else -> if (item.app.componentKey >= other.app.componentKey) 1f else -1f
                        }
                    } else {
                        pitch = (other.pitchDeg + needP * if (dp >= 0f) 1f else -1f)
                            .coerceIn(-55f, 55f)
                    }
                    moved = true
                }
                if (yaw != item.yawDeg || pitch != item.pitchDeg) {
                    mutable[i] = item.copy(
                        yawDeg = yaw,
                        pitchDeg = pitch,
                        velYawDeg = 0f,
                        velPitchDeg = 0f,
                    )
                }
            }
            if (!moved) return mutable
        }
        // Final guarantee: released icons are at rest even if still slightly tight.
        return mutable.map { item ->
            if (item.app.componentKey in releasedKeys) {
                item.copy(velYawDeg = 0f, velPitchDeg = 0f)
            } else {
                item
            }
        }
    }

    /** Collapse any expanded / fanned pile (empty-desk dismiss). */
    fun collapseOpenPiles(): Boolean {
        if (_piles.value.none { it.showsMembers }) return false
        _piles.value = _piles.value.map {
            if (it.showsMembers) DeskPileOps.collapse(it) else it
        }
        return true
    }

    /**
     * BumpDesk lasso layout: rearrange selected Desktop icons (row / column / grid).
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
