package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.abs
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
    const val HOVER_LIFT = 0.03f
    const val HOVER_PAD_SCALE = 1.28f
    const val DRAWER_SCALE = 1.2f
    /** Face size on the sphere (BumpDesk pancake, XY toward the camera). */
    const val ICON_HALF_WIDTH = 0.13f
    const val ICON_HALF_HEIGHT = 0.16f
    const val ICON_HALF_THICK = 0.012f
    /** BumpDesk expanded-pile page: 4×4. */
    const val DRAWER_COLS = 4
    const val DRAWER_PAGE_SIZE = 16

    fun iconHalfWidth(uiScale: Float): Float =
        ICON_HALF_WIDTH * uiScale.coerceAtLeast(0.01f)

    fun iconHalfHeight(uiScale: Float): Float =
        ICON_HALF_HEIGHT * uiScale.coerceAtLeast(0.01f)

    enum class Kind { APP_DRAWER, APP }

    data class AppRef(
        val componentKey: String,
        val label: String,
        val packageName: String,
        val kind: Kind = Kind.APP,
    )

    data class Icon(
        val app: AppRef,
        val yawDeg: Float,
        val pitchDeg: Float,
        val halfWidth: Float = ICON_HALF_WIDTH,
        val halfHeight: Float = ICON_HALF_HEIGHT,
        val halfThick: Float = ICON_HALF_THICK,
        val center: Vec3,
    ) {
        val componentKey: String get() = app.componentKey
        val label: String get() = app.label
        val packageName: String get() = app.packageName
        val kind: Kind get() = app.kind
        val isAppDrawer: Boolean get() = kind == Kind.APP_DRAWER
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
    ): Icon = Icon(
        app = app,
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        halfWidth = halfWidth,
        halfHeight = halfHeight,
        halfThick = halfThick,
        center = pointOnSphere(yawDeg, pitchDeg, sphereScale),
    )

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
        placed: List<AppRef>,
        sphereScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        uiScale: Float = 1f,
        drawerOpen: Boolean = false,
        drawerApps: List<AppRef> = emptyList(),
        drawerPage: Int = 0,
    ): List<Icon> {
        val scale = sphereScale.coerceAtLeast(0.01f)
        val iconScale = uiScale.coerceAtLeast(0.01f)
        val halfW = iconHalfWidth(iconScale)
        val halfH = iconHalfHeight(iconScale)
        val yaw = yawDegrees(viewportWidthPx, viewportHeightPx, panelScale, scale)
        val drawer = iconOf(
            app = AppRef(
                componentKey = DRAWER_KEY,
                label = DRAWER_LABEL,
                packageName = "",
                kind = Kind.APP_DRAWER,
            ),
            yawDeg = yaw,
            pitchDeg = 0f,
            sphereScale = scale,
            halfWidth = halfW * DRAWER_SCALE,
            halfHeight = halfH * DRAWER_SCALE,
        )
        val radius = HomeSpaceScene.innerSphereRadius(scale).coerceAtLeast(0.01f)
        val yawStep = Math.toDegrees((halfW * 2.4f / radius).toDouble()).toFloat()
        val pitchStep = Math.toDegrees((halfH * 2.4f / radius).toDouble()).toFloat()
        val placedIcons = placed.filter { it.kind == Kind.APP }.mapIndexed { index, app ->
            val col = (index % DRAWER_COLS) - 1.5f
            val row = 1 + index / DRAWER_COLS
            iconOf(
                app = app,
                yawDeg = yaw + col * yawStep,
                pitchDeg = -row * pitchStep,
                sphereScale = scale,
                halfWidth = halfW,
                halfHeight = halfH,
            )
        }
        val placedKeys = placedIcons.map { it.componentKey }.toSet()
        val openIcons = if (!drawerOpen) {
            emptyList()
        } else {
            val page = drawerPage.coerceAtLeast(0)
            drawerApps
                .filter { it.kind == Kind.APP && it.componentKey !in placedKeys }
                .drop(page * DRAWER_PAGE_SIZE)
                .take(DRAWER_PAGE_SIZE)
                .mapIndexed { index, app ->
                    val col = index % DRAWER_COLS
                    val row = index / DRAWER_COLS
                    iconOf(
                        app = app,
                        yawDeg = yaw + (col - 1.5f) * yawStep,
                        pitchDeg = (1.5f - row) * pitchStep,
                        sphereScale = scale,
                        halfWidth = halfW,
                        halfHeight = halfH,
                    )
                }
        }
        return listOf(drawer) + placedIcons + openIcons
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
        )

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
            center.x + right.x * sx * icon.halfWidth * HOVER_PAD_SCALE +
                up.x * sy * icon.halfHeight * HOVER_PAD_SCALE,
            center.y + right.y * sx * icon.halfWidth * HOVER_PAD_SCALE +
                up.y * sy * icon.halfHeight * HOVER_PAD_SCALE,
            center.z + right.z * sx * icon.halfWidth * HOVER_PAD_SCALE +
                up.z * sy * icon.halfHeight * HOVER_PAD_SCALE,
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
        quad(face.outBr, face.outBl, face.outTl, face.outTr, out, textured = false)
        quad(face.bl, face.tl, face.outTl, face.outBl, up * -1f, textured = false)
        quad(face.tr, face.br, face.outBr, face.outTr, up, textured = false)
        quad(face.tl, face.tr, face.outTr, face.outTl, right * -1f, textured = false)
        quad(face.br, face.bl, face.outBl, face.outBr, right, textured = false)
        return HomeSpacePaneMesh(verts.toFloatArray(), 36)
    }
}
