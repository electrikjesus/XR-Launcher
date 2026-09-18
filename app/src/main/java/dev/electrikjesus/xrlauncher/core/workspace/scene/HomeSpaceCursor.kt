package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Ray from the camera through a view pixel, where it meets the inner Home Space sphere. */
data class HomeSpaceSphereHit(
    val world: Vec3,
    val yawDeg: Float,
    val pitchDeg: Float,
)

/** Sphere hit that also lands on a pane; [u]/[v] are 0–1 on that pane's inner face. */
data class HomeSpacePanePick(
    val slot: HomeSpacePaneSlot,
    val u: Float,
    val v: Float,
    val hit: HomeSpaceSphereHit,
)

fun Vec3.normalized(): Vec3 {
    val len = length().coerceAtLeast(1e-6f)
    return Vec3(x / len, y / len, z / len)
}

fun HomeSpaceScene.paneRootKey(panelId: String): String = "__xr_pane_root_${panelId}__"

fun HomeSpaceScene.paneKeyPrefix(panelId: String): String = "$panelId::"

fun HomeSpaceScene.innerSphereRadius(sphereScale: Float = 1f): Float {
    val outer = sphereRadius(sphereScale)
    return (outer - HomeSpacePaneMesh.THICKNESS * sphereScale.coerceAtLeast(0.01f))
        .coerceAtLeast(outer * 0.5f)
}

/**
 * Inverse of [HomeSpaceScene.Camera.viewPoint]: a camera-space direction back into world space.
 */
fun HomeSpaceScene.Camera.worldDirection(view: Vec3): Vec3 {
    val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
    val cosP = cos(pitchRad)
    val sinP = sin(pitchRad)
    val y1 = view.y * cosP + view.z * sinP
    val z1 = -view.y * sinP + view.z * cosP
    val x1 = view.x
    val cosY = cos(yawRad)
    val sinY = sin(yawRad)
    return Vec3(
        x = x1 * cosY - z1 * sinY,
        y = y1,
        z = x1 * sinY + z1 * cosY,
    )
}

/** Camera-space ray through a normalized 0–1 cursor on the glasses view. */
fun HomeSpaceScene.viewRay(
    cursorX: Float,
    cursorY: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): Vec3 {
    val ndcX = cursorX.coerceIn(0f, 1f) * 2f - 1f
    val ndcY = 1f - cursorY.coerceIn(0f, 1f) * 2f
    val fovy = Math.toRadians(FOV_Y_DEGREES.toDouble()).toFloat()
    val tanHalf = tan(fovy / 2f)
    val aspect = viewportWidthPx / viewportHeightPx.coerceAtLeast(1f)
    return Vec3(ndcX * aspect * tanHalf, ndcY * tanHalf, -1f)
}

fun HomeSpaceScene.sphereHit(
    cursorX: Float,
    cursorY: Float,
    camera: HomeSpaceScene.Camera,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    sphereScale: Float = 1f,
): HomeSpaceSphereHit {
    val worldDir = camera.worldDirection(
        viewRay(cursorX, cursorY, viewportWidthPx, viewportHeightPx),
    )
    val hit = worldDir.normalized() * innerSphereRadius(sphereScale)
    return HomeSpaceSphereHit(
        world = hit,
        yawDeg = hit.yawDegrees(),
        pitchDeg = hit.pitchDegrees(),
    )
}

fun HomeSpaceScene.pickPane(
    cursorX: Float,
    cursorY: Float,
    camera: HomeSpaceScene.Camera,
    slots: List<HomeSpacePaneSlot>,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    panelScale: Float = 1f,
    sphereScale: Float = 1f,
): HomeSpacePanePick? {
    val hit = sphereHit(
        cursorX,
        cursorY,
        camera,
        viewportWidthPx,
        viewportHeightPx,
        sphereScale,
    )
    var best: HomeSpacePanePick? = null
    var bestDist = Float.MAX_VALUE
    slots.forEach { slot ->
        val pane = pane(slot.worldX, viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        if (hit.yawDeg < pane.yawMin || hit.yawDeg > pane.yawMax) return@forEach
        if (hit.pitchDeg < pane.pitchMin || hit.pitchDeg > pane.pitchMax) return@forEach
        val uSpan = (pane.yawMax - pane.yawMin).coerceAtLeast(1e-4f)
        val vSpan = (pane.pitchMax - pane.pitchMin).coerceAtLeast(1e-4f)
        val u = ((hit.yawDeg - pane.yawMin) / uSpan).coerceIn(0f, 1f)
        val v = ((hit.pitchDeg - pane.pitchMin) / vSpan).coerceIn(0f, 1f)
        val dist = (hit.yawDeg - pane.yawDeg) * (hit.yawDeg - pane.yawDeg) +
            hit.pitchDeg * hit.pitchDeg
        if (dist < bestDist) {
            bestDist = dist
            best = HomeSpacePanePick(slot = slot, u = u, v = v, hit = hit)
        }
    }
    return best
}

/** Map a pane UV onto the Compose capture card (y down). */
fun HomeSpaceScene.overlayPx(
    pick: HomeSpacePanePick,
    rootLeft: Float,
    rootTop: Float,
    rootWidth: Float,
    rootHeight: Float,
): Pair<Float, Float> =
    (rootLeft + pick.u * rootWidth) to (rootTop + (1f - pick.v) * rootHeight)
