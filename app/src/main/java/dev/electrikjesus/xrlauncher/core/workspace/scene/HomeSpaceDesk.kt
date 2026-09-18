package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * BumpDesk-style floor you look down onto when turning left from Home.
 * Default contents are a single All Apps drawer tile; pulled-out apps sit as thin boxes.
 */
object HomeSpaceDesk {
    const val PANEL_ID = "desktop"
    const val LOOK_PITCH_DEGREES = 34f
    const val DISTANCE = 1.45f
    const val HALF_EXTENT = 1.7f
    /** Pancake thickness (BumpDesk Box Y is ~0.04 vs ~0.5 XZ scale). */
    const val ICON_HALF_Y = 0.012f
    const val ICON_HALF_X = 0.13f
    const val ICON_HALF_Z = 0.16f
    const val DRAWER_KEY = "__desk_all_apps__"
    const val HOVER_LABEL = "Desktop"
    const val DRAWER_LABEL = "All apps"
    const val HOVER_LIFT = 0.02f
    const val HOVER_PAD_SCALE = 1.28f
    const val DRAWER_SCALE = 1.2f

    enum class Kind { APP_DRAWER, APP }

    data class AppRef(
        val componentKey: String,
        val label: String,
        val packageName: String,
        val kind: Kind = Kind.APP,
    )

    data class Icon(
        val app: AppRef,
        val center: Vec3,
        val halfX: Float = ICON_HALF_X,
        val halfY: Float = ICON_HALF_Y,
        val halfZ: Float = ICON_HALF_Z,
    ) {
        val componentKey: String get() = app.componentKey
        val label: String get() = app.label
        val packageName: String get() = app.packageName
        val kind: Kind get() = app.kind
        val isAppDrawer: Boolean get() = kind == Kind.APP_DRAWER
    }

    fun lookPitchDegrees(look: Float): Float =
        (-look).coerceIn(0f, 1f) * LOOK_PITCH_DEGREES

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

    fun origin(
        sphereScale: Float,
        yawDeg: Float,
    ): Vec3 {
        val scale = sphereScale.coerceAtLeast(0.01f)
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitch = Math.toRadians(LOOK_PITCH_DEGREES.toDouble()).toFloat()
        val d = DISTANCE * scale
        val cp = cos(pitch)
        return Vec3(
            x = d * sin(yaw) * cp,
            y = -d * sin(pitch),
            z = -d * cos(yaw) * cp,
        )
    }

    fun rightAxis(yawDeg: Float): Vec3 {
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        return Vec3(cos(yaw), 0f, sin(yaw))
    }

    fun awayAxis(yawDeg: Float): Vec3 {
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        return Vec3(sin(yaw), 0f, -cos(yaw))
    }

    fun defaultIcons(
        sphereScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
    ): List<Icon> = layout(
        placed = emptyList(),
        sphereScale = sphereScale,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        panelScale = panelScale,
    )

    fun layout(
        placed: List<AppRef>,
        sphereScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
    ): List<Icon> {
        val scale = sphereScale.coerceAtLeast(0.01f)
        val yaw = yawDegrees(viewportWidthPx, viewportHeightPx, panelScale, scale)
        val origin = origin(scale, yaw)
        val right = rightAxis(yaw)
        val away = awayAxis(yaw)
        val halfY = ICON_HALF_Y
        val drawer = Icon(
            app = AppRef(
                componentKey = DRAWER_KEY,
                label = DRAWER_LABEL,
                packageName = "",
                kind = Kind.APP_DRAWER,
            ),
            center = Vec3(origin.x, origin.y + halfY, origin.z),
            halfX = ICON_HALF_X * scale * DRAWER_SCALE,
            halfY = halfY,
            halfZ = ICON_HALF_Z * scale * DRAWER_SCALE,
        )
        val placedIcons = placed.filter { it.kind == Kind.APP }.mapIndexed { index, app ->
            val col = (index % 4) - 1.5f
            val row = 1 + index / 4
            Icon(
                app = app,
                center = Vec3(
                    x = origin.x + right.x * col * 0.34f * scale + away.x * row * 0.40f * scale,
                    y = origin.y + halfY,
                    z = origin.z + right.z * col * 0.34f * scale + away.z * row * 0.40f * scale,
                ),
                halfX = ICON_HALF_X * scale,
                halfY = halfY,
                halfZ = ICON_HALF_Z * scale,
            )
        }
        return listOf(drawer) + placedIcons
    }

    fun planeHit(
        camera: HomeSpaceScene.Camera,
        cursorX: Float,
        cursorY: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        sphereScale: Float = 1f,
        panelScale: Float = 1f,
    ): Vec3? {
        val dir = camera.worldDirection(
            HomeSpaceScene.viewRay(cursorX, cursorY, viewportWidthPx, viewportHeightPx),
        ).normalized()
        if (dir.y > -1e-4f) return null
        val yaw = yawDegrees(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        val planeY = origin(sphereScale, yaw).y
        val t = planeY / dir.y
        if (t <= 0.05f) return null
        return Vec3(dir.x * t, planeY, dir.z * t)
    }

    fun containsHit(
        hit: Vec3,
        sphereScale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
    ): Boolean {
        val yaw = yawDegrees(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        val origin = origin(sphereScale, yaw)
        val right = rightAxis(yaw)
        val away = awayAxis(yaw)
        val dx = hit.x - origin.x
        val dz = hit.z - origin.z
        val localX = dx * right.x + dz * right.z
        val localZ = dx * away.x + dz * away.z
        val extent = HALF_EXTENT * sphereScale.coerceAtLeast(0.01f)
        return localX >= -extent && localX <= extent &&
            localZ >= -extent && localZ <= extent
    }

    fun pickIcon(hit: Vec3, icons: List<Icon>, yawDeg: Float): Icon? {
        val right = rightAxis(yawDeg)
        val away = awayAxis(yawDeg)
        var best: Icon? = null
        var bestDist = Float.MAX_VALUE
        icons.forEach { icon ->
            val dx = hit.x - icon.center.x
            val dz = hit.z - icon.center.z
            val localX = dx * right.x + dz * right.z
            val localZ = dx * away.x + dz * away.z
            if (abs(localX) > icon.halfX * 1.12f) return@forEach
            if (abs(localZ) > icon.halfZ * 1.12f) return@forEach
            val dist = localX * localX + localZ * localZ
            if (dist < bestDist) {
                bestDist = dist
                best = icon
            }
        }
        return best
    }

    fun planeMesh(sphereScale: Float, yawDeg: Float): HomeSpacePaneMesh {
        val origin = origin(sphereScale, yawDeg)
        val right = rightAxis(yawDeg)
        val away = awayAxis(yawDeg)
        val extent = HALF_EXTENT * sphereScale.coerceAtLeast(0.01f)
        val y = origin.y
        fun corner(sx: Float, sz: Float) = Vec3(
            origin.x + right.x * sx * extent + away.x * sz * extent,
            y,
            origin.z + right.z * sx * extent + away.z * sz * extent,
        )
        val bl = corner(-1f, -1f)
        val br = corner(1f, -1f)
        val tl = corner(-1f, 1f)
        val tr = corner(1f, 1f)
        val n = Vec3(0f, 1f, 0f)
        val verts = ArrayList<Float>(6 * HomeSpacePaneMesh.STRIDE)
        fun add(p: Vec3, u: Float, v: Float) {
            verts += p.x
            verts += p.y
            verts += p.z
            verts += n.x
            verts += n.y
            verts += n.z
            verts += u
            verts += v
        }
        add(bl, -1f, 0f)
        add(br, -1f, 0f)
        add(tl, -1f, 0f)
        add(tl, -1f, 0f)
        add(br, -1f, 0f)
        add(tr, -1f, 0f)
        return HomeSpacePaneMesh(verts.toFloatArray(), 6)
    }

    fun hoverPadMesh(icon: Icon, yawDeg: Float): HomeSpacePaneMesh {
        val right = rightAxis(yawDeg)
        val away = awayAxis(yawDeg)
        val n = Vec3(0f, 1f, 0f)
        val y = icon.center.y - icon.halfY + 0.0015f
        fun corner(sx: Float, sz: Float) = Vec3(
            icon.center.x + right.x * sx * icon.halfX * HOVER_PAD_SCALE +
                away.x * sz * icon.halfZ * HOVER_PAD_SCALE,
            y,
            icon.center.z + right.z * sx * icon.halfX * HOVER_PAD_SCALE +
                away.z * sz * icon.halfZ * HOVER_PAD_SCALE,
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
            verts += n.x
            verts += n.y
            verts += n.z
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

    fun iconMesh(icon: Icon, yawDeg: Float, liftY: Float = 0f): HomeSpacePaneMesh {
        val right = rightAxis(yawDeg)
        val away = awayAxis(yawDeg)
        val up = Vec3(0f, 1f, 0f)
        fun point(sx: Float, sy: Float, sz: Float) = Vec3(
            icon.center.x + right.x * sx * icon.halfX + away.x * sz * icon.halfZ,
            icon.center.y + liftY + sy * icon.halfY,
            icon.center.z + right.z * sx * icon.halfX + away.z * icon.halfZ * sz,
        )
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
                add(a, n, 0f, 1f)
                add(b, n, 1f, 1f)
                add(c, n, 1f, 0f)
                add(a, n, 0f, 1f)
                add(c, n, 1f, 0f)
                add(d, n, 0f, 0f)
            } else {
                add(a, n, -1f, 0f)
                add(b, n, -1f, 0f)
                add(c, n, -1f, 0f)
                add(a, n, -1f, 0f)
                add(c, n, -1f, 0f)
                add(d, n, -1f, 0f)
            }
        }
        val p000 = point(-1f, -1f, -1f)
        val p100 = point(1f, -1f, -1f)
        val p110 = point(1f, 1f, -1f)
        val p010 = point(-1f, 1f, -1f)
        val p001 = point(-1f, -1f, 1f)
        val p101 = point(1f, -1f, 1f)
        val p111 = point(1f, 1f, 1f)
        val p011 = point(-1f, 1f, 1f)
        quad(p011, p111, p110, p010, up, textured = true)
        quad(p000, p100, p101, p001, up * -1f, textured = false)
        quad(p001, p101, p111, p011, away, textured = false)
        quad(p100, p000, p010, p110, away * -1f, textured = false)
        quad(p000, p001, p011, p010, right * -1f, textured = false)
        quad(p101, p100, p110, p111, right, textured = false)
        return HomeSpacePaneMesh(verts.toFloatArray(), 36)
    }
}
