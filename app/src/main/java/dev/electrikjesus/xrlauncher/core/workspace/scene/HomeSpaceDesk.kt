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
            halfWidth = ICON_HALF_WIDTH * DRAWER_SCALE,
            halfHeight = ICON_HALF_HEIGHT * DRAWER_SCALE,
        )
        val placedIcons = placed.filter { it.kind == Kind.APP }.mapIndexed { index, app ->
            val col = (index % 4) - 1.5f
            val row = 1 + index / 4
            iconOf(
                app = app,
                yawDeg = yaw + col * 8f,
                pitchDeg = -row * 7f,
                sphereScale = scale,
            )
        }
        return listOf(drawer) + placedIcons
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

    fun iconMesh(icon: Icon, lift: Float = 0f): HomeSpacePaneMesh {
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
        val pInBl = point(-1f, -1f, -1f)
        val pInBr = point(1f, -1f, -1f)
        val pInTr = point(1f, 1f, -1f)
        val pInTl = point(-1f, 1f, -1f)
        val pOutBl = point(-1f, -1f, 1f)
        val pOutBr = point(1f, -1f, 1f)
        val pOutTr = point(1f, 1f, 1f)
        val pOutTl = point(-1f, 1f, 1f)
        quad(pInBl, pInBr, pInTr, pInTl, inward, textured = true)
        quad(pOutBr, pOutBl, pOutTl, pOutTr, out, textured = false)
        quad(pInBl, pInTl, pOutTl, pOutBl, up * -1f, textured = false)
        quad(pInTr, pInBr, pOutBr, pOutTr, up, textured = false)
        quad(pInTl, pInTr, pOutTr, pOutTl, right * -1f, textured = false)
        quad(pInBr, pInBl, pOutBl, pOutBr, right, textured = false)
        return HomeSpacePaneMesh(verts.toFloatArray(), 36)
    }
}
