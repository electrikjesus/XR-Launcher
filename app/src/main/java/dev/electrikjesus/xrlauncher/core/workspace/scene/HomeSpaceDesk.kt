package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * BumpDesk-style icons living on the inner Home Space sphere — there is no floor.
 * Looking left faces the All Apps tile the same way Home/Tray face their panes.
 */
object HomeSpaceDesk {
    const val PANEL_ID = "desktop"
    const val DRAWER_KEY = "__desk_all_apps__"
    const val HOVER_LABEL = "Desktop"
    const val DRAWER_LABEL = "All apps"
    const val HOVER_LIFT = 0.04f
    const val DRAWER_SCALE = 1.15f
    /**
     * Home pane [AppIconCell] size ([GlassesHomeSpace] grid). Desktop GLES faces are
     * sized to match this under the same Icons & elements ([WorkspaceAppearance.uiScale]).
     */
    const val MATCH_HOME_ICON_DP = 92f
    /**
     * Fallback half-extents at uiScale 1 when viewport/density are unknown — keep in sync
     * with [matchedIconHalfExtent] at 1920×1080 / panel 0.70 / density 2 / sphere 1.
     */
    const val ICON_HALF_WIDTH = 0.162f
    const val ICON_HALF_HEIGHT = 0.162f
    const val ICON_HALF_THICK = 0.010f
    /**
     * World height/width for Desktop app faces that include a label strip.
     * Must match [dev.electrikjesus.xrlauncher.ui.spatial.gles.DeskIconBitmaps.LABELED_ASPECT]
     * (BumpDesk APP heightMult ≈ 1.25) or round icons read as vertical ovals.
     */
    const val LABELED_ICON_ASPECT = 1.25f
    const val DRAWER_COLS = 5
    const val DRAWER_ROWS = 4
    const val DRAWER_PAGE_SIZE = DRAWER_COLS * DRAWER_ROWS
    const val BACKING_KEY = "__desk_all_apps_widget__"
    const val PAGE_PREV_KEY = "__desk_page_prev__"
    const val PAGE_NEXT_KEY = "__desk_page_next__"
    /** Open-drawer tiles — same face size as Desktop apps (uniform app class). */
    const val DRAWER_OPEN_ICON_SCALE = 1.0f
    /** Horizontal arc spacing between open-drawer icon centers, in icon widths. */
    const val DRAWER_OPEN_COL_SPACING = 2.45f
    /** Vertical arc spacing between open-drawer icon centers, in icon heights. */
    const val DRAWER_OPEN_ROW_SPACING = 2.55f
    /** Extra pitch gap (in row-spacing units) between bottom icon row and pager. */
    const val DRAWER_PAGER_GAP = 0.85f
    /** Padding around grid+pager inside the backing, in icon half-sizes. */
    const val DRAWER_BACKING_PAD = 0.10f
    /** Horizontal pad past the icon/chevron outer edge (icon half-sizes). */
    const val DRAWER_BACKING_WIDTH_PAD = 0.04f
    const val MAX_PAGE_DOTS = 5
    /**
     * Soft cap on open-drawer angular half-height (degrees) so the pager stays
     * inside companion mouse-look / cursor pitch reach ([HomeSpaceScene.MAX_PITCH_DEGREES]).
     */
    const val DRAWER_MAX_HALF_PITCH_DEG = 16f
    /** Inward lift of the expanded All Apps widget (closer to the camera). */
    const val BACKING_LIFT = 0.11f
    /** Extra lift so app icons and pager sit on top of the widget. */
    const val ICON_STACK_LIFT = 0.05f
    /** Minimum angular separation between desktop icons (degrees). */
    const val ICON_COLLISION_DEG = 5.5f

    /** @deprecated Use [DRAWER_OPEN_COL_SPACING] / [DRAWER_OPEN_ROW_SPACING]. */
    const val DRAWER_OPEN_SPACING = DRAWER_OPEN_COL_SPACING

    fun pageKey(index: Int): String = "__desk_page_${index}__"

    fun pageIndex(componentKey: String): Int? {
        if (!componentKey.startsWith("__desk_page_") || !componentKey.endsWith("__")) return null
        return componentKey.removePrefix("__desk_page_").removeSuffix("__").toIntOrNull()
    }

    /**
     * World half-extent for a square Desktop face that matches a Home pane icon of
     * [MATCH_HOME_ICON_DP] under the same [uiScale] (Icons & elements).
     *
     * Uses the live Home pane geometry so desk and panel icons stay glued when panel /
     * sphere scale or viewport change — not only when uiScale nudges.
     */
    fun matchedIconHalfExtent(
        uiScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float,
        sphereScale: Float,
        density: Float,
        iconDp: Float = MATCH_HOME_ICON_DP,
    ): Float {
        val pane = HomeSpaceScene.pane(
            worldX = 0f,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            panelScale = panelScale,
            sphereScale = sphereScale,
        )
        val halfHWorld = pane.corners.maxOf { abs(it.y - pane.center.y) }.coerceAtLeast(1e-4f)
        val paneHPx = (
            viewportHeightPx.coerceAtLeast(1f) *
                HomeSpaceScene.paneHeightFraction(panelScale)
            ).coerceAtLeast(1f)
        val iconPx = iconDp *
            density.coerceAtLeast(0.5f) *
            uiScale.coerceAtLeast(0.01f)
        return (iconPx / paneHPx) * halfHWorld
    }

    fun iconHalfWidth(uiScale: Float): Float =
        ICON_HALF_WIDTH * uiScale.coerceAtLeast(0.01f)

    fun iconHalfHeight(uiScale: Float): Float =
        ICON_HALF_HEIGHT * uiScale.coerceAtLeast(0.01f)

    fun iconHalfWidth(
        uiScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float,
        sphereScale: Float,
        density: Float,
    ): Float = matchedIconHalfExtent(
        uiScale = uiScale,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        panelScale = panelScale,
        sphereScale = sphereScale,
        density = density,
    )

    fun iconHalfHeight(
        uiScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float,
        sphereScale: Float,
        density: Float,
    ): Float = iconHalfWidth(
        uiScale,
        viewportWidthPx,
        viewportHeightPx,
        panelScale,
        sphereScale,
        density,
    )

    /** Half-height for icon+label Desktop faces (square icon circle stays round). */
    fun labeledIconHalfHeight(iconHalfWidth: Float): Float =
        iconHalfWidth * LABELED_ICON_ASPECT

    enum class Kind {
        APP_DRAWER,
        APP,
        DRAWER_BACKING,
        PAGE_PREV,
        PAGE_NEXT,
        PAGE,
        WIDGET,
        GROUP_HANDLE,
        /** Corner handle while resizing a desk widget. */
        RESIZE_HANDLE,
        /** Collapsed BumpDesk stack pile face. */
        PILE_STACK,
        /** Collapsed BumpDesk folder pile face (2×2 preview). */
        PILE_FOLDER,
        /** Expanded pile content backing (pick to collapse). */
        PILE_BACKING,
    }

    data class AppRef(
        val componentKey: String,
        val label: String,
        val packageName: String,
        val kind: Kind = Kind.APP,
    )

    data class Placed(
        val app: AppRef,
        val yawDeg: Float,
        val pitchDeg: Float,
        val velYawDeg: Float = 0f,
        val velPitchDeg: Float = 0f,
        /** When set (widgets), layout uses these instead of matched icon half-extents. */
        val halfWidth: Float? = null,
        val halfHeight: Float? = null,
    )

    data class Icon(
        val app: AppRef,
        val yawDeg: Float,
        val pitchDeg: Float,
        val halfWidth: Float = ICON_HALF_WIDTH,
        val halfHeight: Float = ICON_HALF_HEIGHT,
        val halfThick: Float = ICON_HALF_THICK,
        val lift: Float = 0f,
        val center: Vec3,
    ) {
        val componentKey: String get() = app.componentKey
        val label: String get() = app.label
        val packageName: String get() = app.packageName
        val kind: Kind get() = app.kind
        val isAppDrawer: Boolean get() = kind == Kind.APP_DRAWER
        val isBacking: Boolean get() = kind == Kind.DRAWER_BACKING
        val isPager: Boolean get() = kind == Kind.PAGE_PREV || kind == Kind.PAGE_NEXT || kind == Kind.PAGE
        val isDesktopApp: Boolean get() = kind == Kind.APP
        val isWidget: Boolean get() = kind == Kind.WIDGET
        val isGroupHandle: Boolean get() = kind == Kind.GROUP_HANDLE
        val isResizeHandle: Boolean get() = kind == Kind.RESIZE_HANDLE
        val isPileFace: Boolean get() = kind == Kind.PILE_STACK || kind == Kind.PILE_FOLDER
        val isPileBacking: Boolean get() = kind == Kind.PILE_BACKING
    }

    fun yawDegrees(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): Float = HomeSpaceScene.pane(
        worldX = -1f,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        panelScale = panelScale,
        sphereScale = sphereScale,
    ).yawDeg

    fun pointOnSphere(
        yawDeg: Float,
        pitchDeg: Float,
        sphereScale: Float,
    ): Vec3 {
        val r = HomeSpaceScene.innerSphereRadius(sphereScale)
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val cp = cos(pitch)
        return Vec3(
            x = r * sin(yaw) * cp,
            y = r * sin(pitch),
            z = -r * cos(yaw) * cp,
        )
    }

    fun outward(yawDeg: Float, pitchDeg: Float): Vec3 =
        pointOnSphere(yawDeg, pitchDeg, 1f).normalized()

    fun rightAxis(yawDeg: Float): Vec3 {
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        return Vec3(cos(yaw), 0f, sin(yaw))
    }

    fun upAxis(yawDeg: Float, pitchDeg: Float): Vec3 {
        val right = rightAxis(yawDeg)
        val out = outward(yawDeg, pitchDeg)
        return Vec3(
            right.y * out.z - right.z * out.y,
            right.z * out.x - right.x * out.z,
            right.x * out.y - right.y * out.x,
        ).normalized()
    }

    fun iconOf(
        app: AppRef,
        yawDeg: Float,
        pitchDeg: Float,
        sphereScale: Float,
        halfWidth: Float = ICON_HALF_WIDTH,
        halfHeight: Float = ICON_HALF_HEIGHT,
        halfThick: Float = ICON_HALF_THICK,
        lift: Float = 0f,
    ): Icon {
        val onWall = pointOnSphere(yawDeg, pitchDeg, sphereScale)
        val inward = outward(yawDeg, pitchDeg) * -1f
        return Icon(
            app = app,
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            halfWidth = halfWidth,
            halfHeight = halfHeight,
            halfThick = halfThick,
            lift = lift,
            center = onWall + inward * lift,
        )
    }

    fun defaultIcons(
        sphereScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        uiScale: Float = 1f,
        drawerOpen: Boolean = false,
        drawerApps: List<AppRef> = emptyList(),
        drawerPage: Int = 0,
    ): List<Icon> = layout(
        placed = emptyList(),
        sphereScale = sphereScale,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        panelScale = panelScale,
        uiScale = uiScale,
        drawerOpen = drawerOpen,
        drawerApps = drawerApps,
        drawerPage = drawerPage,
    )

    fun layout(
        placed: List<Placed>,
        sphereScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        uiScale: Float = 1f,
        density: Float = 2f,
        drawerOpen: Boolean = false,
        drawerApps: List<AppRef> = emptyList(),
        drawerPage: Int = 0,
        draggingKey: String? = null,
        dragYawDeg: Float = 0f,
        dragPitchDeg: Float = 0f,
        drawerYawDeg: Float? = null,
        drawerPitchDeg: Float = 0f,
    ): List<Icon> {
        val scale = sphereScale.coerceAtLeast(0.01f)
        val halfW = iconHalfWidth(
            uiScale = uiScale,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            panelScale = panelScale,
            sphereScale = scale,
            density = density,
        )
        // Labeled APP / APP_DRAWER textures are taller than wide — stretch mesh height so
        // the circular icon region stays round (BumpDesk heightMult).
        val labeledHalfH = labeledIconHalfHeight(halfW)
        val defaultYaw = yawDegrees(viewportWidthPx, viewportHeightPx, panelScale, scale)
        val yaw = drawerYawDeg ?: defaultYaw
        val radius = HomeSpaceScene.innerSphereRadius(scale).coerceAtLeast(0.01f)
        val drawerDragging = draggingKey == DRAWER_KEY
        val drawer = iconOf(
            app = AppRef(
                componentKey = DRAWER_KEY,
                label = DRAWER_LABEL,
                packageName = "",
                kind = Kind.APP_DRAWER,
            ),
            yawDeg = if (drawerDragging) dragYawDeg else yaw,
            pitchDeg = if (drawerDragging) dragPitchDeg else drawerPitchDeg,
            sphereScale = scale,
            halfWidth = halfW * DRAWER_SCALE,
            halfHeight = labeledHalfH * DRAWER_SCALE,
            lift = if (drawerDragging) HOVER_LIFT else 0f,
        )
        val placedIcons = placed.filter {
            it.app.kind == Kind.APP || it.app.kind == Kind.WIDGET
        }.map { item ->
            val pose = if (item.app.componentKey == draggingKey) {
                item.copy(yawDeg = dragYawDeg, pitchDeg = dragPitchDeg)
            } else {
                item
            }
            val itemHalfW = when {
                pose.app.kind == Kind.WIDGET -> pose.halfWidth ?: (halfW * 3.2f)
                else -> halfW
            }
            val itemHalfH = when {
                pose.app.kind == Kind.WIDGET -> pose.halfHeight ?: itemHalfW
                else -> labeledHalfH
            }
            iconOf(
                app = pose.app,
                yawDeg = pose.yawDeg,
                pitchDeg = pose.pitchDeg,
                sphereScale = scale,
                halfWidth = itemHalfW,
                halfHeight = itemHalfH,
                lift = if (item.app.componentKey == draggingKey) HOVER_LIFT else 0f,
            )
        }
        val placedKeys = placed.map { it.app.componentKey }.toSet()
        if (!drawerOpen) return listOf(drawer) + placedIcons

        val page = drawerPage.coerceAtLeast(0)
        val unplaced = drawerApps.filter { it.kind == Kind.APP && it.componentKey !in placedKeys }
        val pageApps = unplaced.drop(page * DRAWER_PAGE_SIZE).take(DRAWER_PAGE_SIZE)
        val pageCount = ((unplaced.size + DRAWER_PAGE_SIZE - 1) / DRAWER_PAGE_SIZE).coerceAtLeast(1)
        // Fit open-drawer faces into the pitch FOV budget when Home-matched icons are large.
        var openHalfW = halfW * DRAWER_OPEN_ICON_SCALE
        var openHalfH = labeledIconHalfHeight(openHalfW)
        fun drawerHalfPitch(halfW: Float, halfH: Float): Float {
            val pitchStep = Math.toDegrees((halfH * DRAWER_OPEN_ROW_SPACING / radius).toDouble()).toFloat()
            val pagerHalfH = halfW * 0.52f // pagers are square (no label strip)
            val gridTop = (DRAWER_ROWS - 1) * 0.5f * pitchStep
            val pagerPitch = -gridTop - (DRAWER_PAGER_GAP + 0.5f) * pitchStep
            val topEdge = gridTop +
                Math.toDegrees((halfH * (1f + DRAWER_BACKING_PAD) / radius).toDouble()).toFloat()
            val bottomEdge = pagerPitch -
                Math.toDegrees((pagerHalfH * (1f + DRAWER_BACKING_PAD) / radius).toDouble()).toFloat()
            return (topEdge - bottomEdge) * 0.5f
        }
        val rawHalfPitch = drawerHalfPitch(openHalfW, openHalfH)
        if (rawHalfPitch > DRAWER_MAX_HALF_PITCH_DEG) {
            val fit = (DRAWER_MAX_HALF_PITCH_DEG / rawHalfPitch).coerceIn(0.35f, 1f)
            openHalfW *= fit
            openHalfH *= fit
        }
        val openYawStep = Math.toDegrees((openHalfW * DRAWER_OPEN_COL_SPACING / radius).toDouble()).toFloat()
        val openPitchStep = Math.toDegrees((openHalfH * DRAWER_OPEN_ROW_SPACING / radius).toDouble()).toFloat()
        // Pagers use square textures (no label) — keep mesh square so chevrons/dots stay round.
        val pagerHalfW = openHalfW * 0.68f
        val pagerHalfH = pagerHalfW
        val dotHalf = openHalfW * 0.36f
        // Grid row centers: +1.5 … -1.5 steps. Pager one clear gap below the bottom row.
        val gridTopPitch = (DRAWER_ROWS - 1) * 0.5f * openPitchStep
        val gridBottomPitch = -gridTopPitch
        val pagerPitch = gridBottomPitch - (DRAWER_PAGER_GAP + 0.5f) * openPitchStep
        val topEdgePitchRaw = gridTopPitch +
            Math.toDegrees((openHalfH * (1f + DRAWER_BACKING_PAD) / radius).toDouble()).toFloat()
        val bottomEdgePitchRaw = pagerPitch -
            Math.toDegrees((pagerHalfH * (1f + DRAWER_BACKING_PAD) / radius).toDouble()).toFloat()
        // Lift the whole open drawer so the pager stays above the cursor pitch floor.
        val pitchShift = ((-DRAWER_MAX_HALF_PITCH_DEG) - bottomEdgePitchRaw).coerceAtLeast(0f)
        val topEdgePitch = topEdgePitchRaw + pitchShift
        val bottomEdgePitch = bottomEdgePitchRaw + pitchShift
        val gridPitchShift = pitchShift
        // Content half-width from outer icon edges; chevrons sit flush inside that span.
        val gridHalfYaw = (DRAWER_COLS - 1) * 0.5f * openYawStep +
            Math.toDegrees((openHalfW / radius).toDouble()).toFloat()
        val pagerHalfYaw = Math.toDegrees((pagerHalfW / radius).toDouble()).toFloat()
        val pagerYaw = (gridHalfYaw - pagerHalfYaw).coerceAtLeast(0f)
        val contentHalfYaw = maxOf(gridHalfYaw, pagerYaw + pagerHalfYaw)
        val sideEdgeYaw = contentHalfYaw +
            Math.toDegrees(
                (openHalfW * DRAWER_BACKING_WIDTH_PAD / radius).toDouble(),
            ).toFloat()
        val contentCenterPitch = (topEdgePitch + bottomEdgePitch) * 0.5f
        val backingHalfW = radius * Math.toRadians(sideEdgeYaw.toDouble()).toFloat()
        var backingHalfH = radius * Math.toRadians(
            ((topEdgePitch - bottomEdgePitch) * 0.5f).toDouble(),
        ).toFloat()
        val maxHalfH = radius * Math.toRadians(DRAWER_MAX_HALF_PITCH_DEG.toDouble()).toFloat()
        backingHalfH = minOf(backingHalfH, maxHalfH)
        val backing = iconOf(
            app = AppRef(BACKING_KEY, DRAWER_LABEL, "", Kind.DRAWER_BACKING),
            yawDeg = yaw,
            pitchDeg = contentCenterPitch,
            sphereScale = scale,
            halfWidth = backingHalfW,
            halfHeight = backingHalfH,
            lift = BACKING_LIFT,
        )
        val stackLift = BACKING_LIFT + ICON_STACK_LIFT
        val openIcons = pageApps.mapIndexed { index, app ->
            val col = index % DRAWER_COLS
            val row = index / DRAWER_COLS
            val iconYaw = yaw + (col - (DRAWER_COLS - 1) * 0.5f) * openYawStep
            val iconPitch = gridTopPitch - row * openPitchStep + gridPitchShift
            val dragging = app.componentKey == draggingKey
            iconOf(
                app = app,
                yawDeg = if (dragging) dragYawDeg else iconYaw,
                pitchDeg = if (dragging) dragPitchDeg else iconPitch,
                sphereScale = scale,
                halfWidth = openHalfW,
                halfHeight = openHalfH,
                lift = if (dragging) stackLift + 0.03f else stackLift,
            )
        }
        val firstDot = page.coerceIn(0, (pageCount - MAX_PAGE_DOTS).coerceAtLeast(0))
        val visibleDots = (pageCount - firstDot).coerceAtMost(MAX_PAGE_DOTS)
        val pagerPitchShifted = pagerPitch + gridPitchShift
        val pager = buildList {
            add(
                iconOf(
                    app = AppRef(PAGE_PREV_KEY, "Previous", "", Kind.PAGE_PREV),
                    yawDeg = yaw - pagerYaw,
                    pitchDeg = pagerPitchShifted,
                    sphereScale = scale,
                    halfWidth = pagerHalfW,
                    halfHeight = pagerHalfH,
                    lift = stackLift,
                ),
            )
            repeat(visibleDots) { offset ->
                val index = firstDot + offset
                add(
                    iconOf(
                        app = AppRef(pageKey(index), "${index + 1}", "", Kind.PAGE),
                        yawDeg = yaw + (offset - (visibleDots - 1) * 0.5f) * openYawStep * 0.48f,
                        pitchDeg = pagerPitchShifted,
                        sphereScale = scale,
                        halfWidth = dotHalf,
                        halfHeight = dotHalf,
                        lift = stackLift,
                    ),
                )
            }
            add(
                iconOf(
                    app = AppRef(PAGE_NEXT_KEY, "Next", "", Kind.PAGE_NEXT),
                    yawDeg = yaw + pagerYaw,
                    pitchDeg = pagerPitchShifted,
                    sphereScale = scale,
                    halfWidth = pagerHalfW,
                    halfHeight = pagerHalfH,
                    lift = stackLift,
                ),
            )
        }
        // Hide the closed All Apps tile while the widget is open — it only cluttered the wall.
        return listOf(backing) + openIcons + pager + placedIcons
    }

    /** Prefer apps/pager over the large backing so pagination stays clickable. */
    private fun pickPriority(icon: Icon): Int = when (icon.kind) {
        Kind.GROUP_HANDLE, Kind.RESIZE_HANDLE -> 0
        Kind.PAGE_PREV, Kind.PAGE_NEXT, Kind.PAGE -> 0
        Kind.APP, Kind.APP_DRAWER, Kind.WIDGET, Kind.PILE_STACK, Kind.PILE_FOLDER -> 1
        Kind.PILE_BACKING -> 2
        Kind.DRAWER_BACKING -> 3
    }

    fun overlapsAngular(
        yawA: Float,
        pitchA: Float,
        halfYawA: Float,
        halfPitchA: Float,
        yawB: Float,
        pitchB: Float,
        halfYawB: Float,
        halfPitchB: Float,
    ): Boolean {
        val dy = abs(yawA - yawB)
        val dp = abs(pitchA - pitchB)
        return dy < halfYawA + halfYawB && dp < halfPitchA + halfPitchB
    }

    fun angularHalfYaw(halfWidth: Float, sphereScale: Float): Float {
        val r = HomeSpaceScene.innerSphereRadius(sphereScale).coerceAtLeast(0.01f)
        return Math.toDegrees((halfWidth / r).toDouble()).toFloat()
    }

    fun angularHalfPitch(halfHeight: Float, sphereScale: Float): Float {
        val r = HomeSpaceScene.innerSphereRadius(sphereScale).coerceAtLeast(0.01f)
        return Math.toDegrees((halfHeight / r).toDouble()).toFloat()
    }

    /**
     * Push [yawDeg]/[pitchDeg] off overlapping desk icons / the open widget / All Apps tile.
     * Returns null if the pose sits on a Home/Tray/app pane.
     */
    fun resolveDesktopDrop(
        yawDeg: Float,
        pitchDeg: Float,
        halfWidth: Float,
        halfHeight: Float,
        sphereScale: Float,
        obstacles: List<Icon>,
        paneBlocks: List<HomeSpaceScene.Pane>,
        excludeKey: String? = null,
    ): Pair<Float, Float>? {
        val halfYaw = angularHalfYaw(halfWidth, sphereScale)
        val halfPitch = angularHalfPitch(halfHeight, sphereScale)
        paneBlocks.forEach { pane ->
            if (yawDeg in pane.yawMin..pane.yawMax && pitchDeg in pane.pitchMin..pane.pitchMax) {
                return null
            }
        }
        var yaw = yawDeg
        var pitch = pitchDeg
        repeat(6) {
            var moved = false
            obstacles.forEach { other ->
                if (other.componentKey == excludeKey) return@forEach
                if (other.isBacking || other.isPager) {
                    val oHalfYaw = angularHalfYaw(other.halfWidth, sphereScale)
                    val oHalfPitch = angularHalfPitch(other.halfHeight, sphereScale)
                    if (overlapsAngular(yaw, pitch, halfYaw, halfPitch, other.yawDeg, other.pitchDeg, oHalfYaw, oHalfPitch)) {
                        return null
                    }
                    return@forEach
                }
                val oHalfYaw = angularHalfYaw(other.halfWidth, sphereScale).coerceAtLeast(ICON_COLLISION_DEG * 0.5f)
                val oHalfPitch = angularHalfPitch(other.halfHeight, sphereScale).coerceAtLeast(ICON_COLLISION_DEG * 0.5f)
                if (!overlapsAngular(yaw, pitch, halfYaw, halfPitch, other.yawDeg, other.pitchDeg, oHalfYaw, oHalfPitch)) {
                    return@forEach
                }
                val dy = yaw - other.yawDeg
                val dp = pitch - other.pitchDeg
                val needY = halfYaw + oHalfYaw + 0.35f
                val needP = halfPitch + oHalfPitch + 0.35f
                when {
                    abs(dy) * needP >= abs(dp) * needY -> {
                        yaw = other.yawDeg + needY * if (dy >= 0f) 1f else -1f
                    }
                    else -> {
                        pitch = other.pitchDeg + needP * if (dp >= 0f) 1f else -1f
                    }
                }
                moved = true
            }
            if (!moved) return yaw to pitch
        }
        return yaw to pitch
    }

    /**
     * True when a drop lands on the closed All Apps tile or the open drawer backing —
     * BumpDesk “return to drawer” removes the Desktop icon.
     */
    fun hitsAllAppsReturn(
        yawDeg: Float,
        pitchDeg: Float,
        halfWidth: Float,
        halfHeight: Float,
        sphereScale: Float,
        obstacles: List<Icon>,
    ): Boolean {
        val halfYaw = angularHalfYaw(halfWidth, sphereScale) * 1.2f
        val halfPitch = angularHalfPitch(halfHeight, sphereScale) * 1.2f
        return obstacles.any { other ->
            if (!other.isAppDrawer && !other.isBacking) return@any false
            val oHalfYaw = angularHalfYaw(other.halfWidth, sphereScale) * 1.2f
            val oHalfPitch = angularHalfPitch(other.halfHeight, sphereScale) * 1.2f
            overlapsAngular(
                yawDeg,
                pitchDeg,
                halfYaw,
                halfPitch,
                other.yawDeg,
                other.pitchDeg,
                oHalfYaw,
                oHalfPitch,
            )
        }
    }

    fun moved(icon: Icon, yawDeg: Float, pitchDeg: Float, sphereScale: Float): Icon =
        iconOf(
            app = icon.app,
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            sphereScale = sphereScale,
            halfWidth = icon.halfWidth,
            halfHeight = icon.halfHeight,
            halfThick = icon.halfThick,
            lift = icon.lift,
        )

    fun pickAlongRay(rayDir: Vec3, icons: List<Icon>): Icon? {
        val dir = rayDir.normalized()
        var best: Icon? = null
        var bestT = Float.MAX_VALUE
        var bestPriority = Int.MAX_VALUE
        icons.forEach { icon ->
            val n = (icon.center * -1f).normalized()
            val denom = dir.dot(n)
            if (abs(denom) < 1e-4f) return@forEach
            val t = icon.center.dot(n) / denom
            if (t < 0.05f) return@forEach
            val hit = dir * t
            val right = rightAxis(icon.yawDeg)
            val up = upAxis(icon.yawDeg, icon.pitchDeg)
            val dx = hit.x - icon.center.x
            val dy = hit.y - icon.center.y
            val dz = hit.z - icon.center.z
            val localX = dx * right.x + dy * right.y + dz * right.z
            val localY = dx * up.x + dy * up.y + dz * up.z
            val slop = when {
                icon.isPager -> 1.85f
                icon.isBacking -> 1.0f
                else -> 1.12f
            }
            if (abs(localX) > icon.halfWidth * slop) return@forEach
            if (abs(localY) > icon.halfHeight * slop) return@forEach
            val priority = pickPriority(icon)
            val closer = t < bestT - 0.01f
            val better = abs(t - bestT) <= 0.08f && priority < bestPriority
            if (best == null || closer || better) {
                bestT = t
                bestPriority = priority
                best = icon
            }
        }
        return best
    }

    /**
     * Generous hit for the open All Apps widget (backing + pager + page apps).
     * Used so near-misses on pagination do not start a Desktop lasso or dismiss the drawer.
     */
    fun inOpenDrawerClickZone(rayDir: Vec3, icons: List<Icon>): Boolean {
        val open = icons.filter { it.isBacking || it.isPager || (it.isDesktopApp && it.lift > 0f) }
        if (open.isEmpty()) return false
        val dir = rayDir.normalized()
        open.forEach { icon ->
            val n = (icon.center * -1f).normalized()
            val denom = dir.dot(n)
            if (abs(denom) < 1e-4f) return@forEach
            val t = icon.center.dot(n) / denom
            if (t < 0.05f) return@forEach
            val hit = dir * t
            val right = rightAxis(icon.yawDeg)
            val up = upAxis(icon.yawDeg, icon.pitchDeg)
            val dx = hit.x - icon.center.x
            val dy = hit.y - icon.center.y
            val dz = hit.z - icon.center.z
            val localX = dx * right.x + dy * right.y + dz * right.z
            val localY = dx * up.x + dy * up.y + dz * up.z
            val slop = when {
                icon.isPager -> 2.15f
                // Match the visible plate so outside-clicks dismiss at the frosted edge.
                icon.isBacking -> 1.0f
                else -> 1.3f
            }
            if (abs(localX) <= icon.halfWidth * slop && abs(localY) <= icon.halfHeight * slop) {
                return true
            }
        }
        return false
    }

    /** Closest pager control under [rayDir], with a wide angular grab for XR cursor jitter. */
    fun pickNearestPager(rayDir: Vec3, icons: List<Icon>, maxAngleDeg: Float = 9f): Icon? {
        val dir = rayDir.normalized()
        var best: Icon? = null
        var bestAngle = maxAngleDeg
        icons.filter { it.isPager }.forEach { icon ->
            val toIcon = icon.center.normalized()
            val dot = dir.dot(toIcon).coerceIn(-1f, 1f)
            val angle = Math.toDegrees(acos(dot.toDouble())).toFloat()
            if (angle < bestAngle) {
                bestAngle = angle
                best = icon
            }
        }
        return best
    }

    fun pickIcon(hit: Vec3, icons: List<Icon>): Icon? {
        var best: Icon? = null
        var bestDist = Float.MAX_VALUE
        icons.forEach { icon ->
            val right = rightAxis(icon.yawDeg)
            val up = upAxis(icon.yawDeg, icon.pitchDeg)
            val dx = hit.x - icon.center.x
            val dy = hit.y - icon.center.y
            val dz = hit.z - icon.center.z
            val localX = dx * right.x + dy * right.y + dz * right.z
            val localY = dx * up.x + dy * up.y + dz * up.z
            if (abs(localX) > icon.halfWidth * 1.15f) return@forEach
            if (abs(localY) > icon.halfHeight * 1.15f) return@forEach
            val dist = localX * localX + localY * localY
            if (dist < bestDist) {
                bestDist = dist
                best = icon
            }
        }
        return best
    }

    fun hoverPadMesh(icon: Icon, lift: Float = 0f): HomeSpacePaneMesh {
        val right = rightAxis(icon.yawDeg)
        val up = upAxis(icon.yawDeg, icon.pitchDeg)
        val inward = outward(icon.yawDeg, icon.pitchDeg) * -1f
        val center = icon.center + inward * (icon.halfThick + 0.004f + lift)
        fun corner(sx: Float, sy: Float) = Vec3(
            center.x + right.x * sx * icon.halfWidth * 1.28f +
                up.x * sy * icon.halfHeight * 1.28f,
            center.y + right.y * sx * icon.halfWidth * 1.28f +
                up.y * sy * icon.halfHeight * 1.28f,
            center.z + right.z * sx * icon.halfWidth * 1.28f +
                up.z * sy * icon.halfHeight * 1.28f,
        )
        val bl = corner(-1f, -1f)
        val br = corner(1f, -1f)
        val tl = corner(-1f, 1f)
        val tr = corner(1f, 1f)
        val verts = ArrayList<Float>(6 * HomeSpacePaneMesh.STRIDE)
        fun add(p: Vec3) {
            verts += p.x
            verts += p.y
            verts += p.z
            verts += inward.x
            verts += inward.y
            verts += inward.z
            verts += -2f
            verts += 0f
        }
        add(bl)
        add(br)
        add(tl)
        add(tl)
        add(br)
        add(tr)
        return HomeSpacePaneMesh(verts.toFloatArray(), 6)
    }

    data class InwardFace(
        val bl: Vec3,
        val br: Vec3,
        val tr: Vec3,
        val tl: Vec3,
        val inward: Vec3,
        val outBl: Vec3,
        val outBr: Vec3,
        val outTr: Vec3,
        val outTl: Vec3,
    )

    fun inwardFace(icon: Icon, lift: Float = 0f): InwardFace {
        val right = rightAxis(icon.yawDeg)
        val up = upAxis(icon.yawDeg, icon.pitchDeg)
        val out = outward(icon.yawDeg, icon.pitchDeg)
        val inward = out * -1f
        val center = icon.center + inward * lift
        fun point(sx: Float, sy: Float, sz: Float) = Vec3(
            center.x + right.x * sx * icon.halfWidth + up.x * sy * icon.halfHeight + out.x * sz * icon.halfThick,
            center.y + right.y * sx * icon.halfWidth + up.y * sy * icon.halfHeight + out.y * sz * icon.halfThick,
            center.z + right.z * sx * icon.halfWidth + up.z * sy * icon.halfHeight + out.z * sz * icon.halfThick,
        )
        return InwardFace(
            bl = point(-1f, -1f, -1f),
            br = point(1f, -1f, -1f),
            tr = point(1f, 1f, -1f),
            tl = point(-1f, 1f, -1f),
            inward = inward,
            outBl = point(-1f, -1f, 1f),
            outBr = point(1f, -1f, 1f),
            outTr = point(1f, 1f, 1f),
            outTl = point(-1f, 1f, 1f),
        )
    }

    fun iconMesh(icon: Icon, lift: Float = 0f): HomeSpacePaneMesh {
        val right = rightAxis(icon.yawDeg)
        val up = upAxis(icon.yawDeg, icon.pitchDeg)
        val out = outward(icon.yawDeg, icon.pitchDeg)
        val inward = out * -1f
        val face = inwardFace(icon, lift)
        val verts = ArrayList<Float>(36 * HomeSpacePaneMesh.STRIDE)
        fun add(p: Vec3, n: Vec3, u: Float, v: Float) {
            verts += p.x
            verts += p.y
            verts += p.z
            verts += n.x
            verts += n.y
            verts += n.z
            verts += u
            verts += v
        }
        fun quad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, n: Vec3, textured: Boolean) {
            if (textured) {
                // Shader samples (u, 1-v). Mesh V=0 is the bottom of the camera-facing
                // pancake so the bitmap top (grid) lands on the top of the tile.
                add(a, n, 0f, 0f)
                add(b, n, 1f, 0f)
                add(c, n, 1f, 1f)
                add(a, n, 0f, 0f)
                add(c, n, 1f, 1f)
                add(d, n, 0f, 1f)
            } else {
                add(a, n, -1f, 0f)
                add(b, n, -1f, 0f)
                add(c, n, -1f, 0f)
                add(a, n, -1f, 0f)
                add(c, n, -1f, 0f)
                add(d, n, -1f, 0f)
            }
        }
        quad(face.bl, face.br, face.tr, face.tl, inward, textured = true)
        // App / All-Apps stickers are round via texture alpha — skip box sides so a
        // rectangular pancake silhouette does not read as a square plate around the icon.
        // Widgets keep a thin pancake (front only for now) with rectangular textures.
        if (icon.isDesktopApp || icon.isAppDrawer || icon.isWidget) {
            return HomeSpacePaneMesh(verts.toFloatArray(), 6)
        }
        quad(face.outBr, face.outBl, face.outTl, face.outTr, out, textured = false)
        quad(face.bl, face.tl, face.outTl, face.outBl, up * -1f, textured = false)
        quad(face.tr, face.br, face.outBr, face.outTr, up, textured = false)
        quad(face.tl, face.tr, face.outTr, face.outTl, right * -1f, textured = false)
        quad(face.br, face.bl, face.outBl, face.outBr, right, textured = false)
        return HomeSpacePaneMesh(verts.toFloatArray(), 36)
    }
}
