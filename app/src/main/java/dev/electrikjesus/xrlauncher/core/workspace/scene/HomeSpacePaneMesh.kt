package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.cos
import kotlin.math.sin

/**
 * Tessellated Home Space pane: a spherical patch with BumpDesk-style thickness
 * so a true FPS camera sees bowed edges and an inner bevel, not a flat billboard.
 */
data class HomeSpacePaneMesh(
    val interleaved: FloatArray,
    val vertexCount: Int,
) {
    fun position(index: Int): Vec3 {
        val base = index * STRIDE
        return Vec3(interleaved[base], interleaved[base + 1], interleaved[base + 2])
    }

    fun frontVertexNear(u: Float, v: Float): Vec3 {
        var best = position(0)
        var bestDist = Float.MAX_VALUE
        for (i in 0 until vertexCount) {
            val base = i * STRIDE
            val vu = interleaved[base + 6]
            val vv = interleaved[base + 7]
            if (vu < 0f) continue
            val dist = (vu - u) * (vu - u) + (vv - v) * (vv - v)
            if (dist < bestDist) {
                bestDist = dist
                best = position(i)
            }
        }
        return best
    }

    fun frontVertices(): List<Vec3> = (0 until vertexCount).mapNotNull { i ->
        val u = interleaved[i * STRIDE + 6]
        if (u >= 0f) position(i) else null
    }

    companion object {
        const val STRIDE = 8
        const val TESSEL_U = 14
        const val TESSEL_V = 10
        const val THICKNESS = 0.06f
    }
}

data class HomeSpacePaneSlot(
    val panelId: String,
    val worldX: Float,
)

fun HomeSpaceScene.spherePoint(yawDeg: Float, pitchDeg: Float, radius: Float): Vec3 {
    val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
    val cp = cos(pitch)
    return Vec3(
        x = radius * sin(yaw) * cp,
        y = radius * sin(pitch),
        z = -radius * cos(yaw) * cp,
    )
}

fun HomeSpaceScene.paneMesh(
    worldX: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    panelScale: Float = 1f,
    sphereScale: Float = 1f,
): HomeSpacePaneMesh {
    val pane = pane(worldX, viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
    val rOuter = sphereRadius(sphereScale)
    val rInner = (rOuter - HomeSpacePaneMesh.THICKNESS * sphereScale).coerceAtLeast(rOuter * 0.5f)
    val verts = ArrayList<Float>(2048)

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

    fun inward(p: Vec3): Vec3 {
        val len = p.length().coerceAtLeast(1e-5f)
        return Vec3(-p.x / len, -p.y / len, -p.z / len)
    }

    fun outward(p: Vec3): Vec3 = inward(p) * -1f

    fun yawAt(i: Int) = pane.yawMin + (pane.yawMax - pane.yawMin) * (i / HomeSpacePaneMesh.TESSEL_U.toFloat())
    fun pitchAt(j: Int) = pane.pitchMin + (pane.pitchMax - pane.pitchMin) * (j / HomeSpacePaneMesh.TESSEL_V.toFloat())

    val uCount = HomeSpacePaneMesh.TESSEL_U
    val vCount = HomeSpacePaneMesh.TESSEL_V
    val inner = Array(uCount + 1) { i ->
        Array(vCount + 1) { j -> spherePoint(yawAt(i), pitchAt(j), rInner) }
    }
    val outer = Array(uCount + 1) { i ->
        Array(vCount + 1) { j -> spherePoint(yawAt(i), pitchAt(j), rOuter) }
    }

    fun quad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, n: Vec3, ua: Float, va: Float, ub: Float, vb: Float, uc: Float, vc: Float, ud: Float, vd: Float) {
        add(a, n, ua, va)
        add(b, n, ub, vb)
        add(c, n, uc, vc)
        add(a, n, ua, va)
        add(c, n, uc, vc)
        add(d, n, ud, vd)
    }

    for (i in 0 until uCount) {
        for (j in 0 until vCount) {
            val u0 = i / uCount.toFloat()
            val u1 = (i + 1) / uCount.toFloat()
            val v0 = j / vCount.toFloat()
            val v1 = (j + 1) / vCount.toFloat()
            val a = inner[i][j]
            val b = inner[i + 1][j]
            val c = inner[i + 1][j + 1]
            val d = inner[i][j + 1]
            quad(a, b, c, d, inward(a), u0, v0, u1, v0, u1, v1, u0, v1)
        }
    }

    val edgeUv = -1f
    fun edgeNormal(a: Vec3, b: Vec3, outerA: Vec3): Vec3 {
        val t = b - a
        val r = outerA - a
        val cx = t.y * r.z - t.z * r.y
        val cy = t.z * r.x - t.x * r.z
        val cz = t.x * r.y - t.y * r.x
        val n = Vec3(cx, cy, cz)
        val len = n.length().coerceAtLeast(1e-5f)
        return Vec3(n.x / len, n.y / len, n.z / len)
    }

    for (j in 0 until vCount) {
        val a = inner[0][j]
        val b = inner[0][j + 1]
        val c = outer[0][j + 1]
        val d = outer[0][j]
        val n = edgeNormal(a, b, d)
        quad(a, b, c, d, n, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f)
    }
    for (j in 0 until vCount) {
        val a = inner[uCount][j + 1]
        val b = inner[uCount][j]
        val c = outer[uCount][j]
        val d = outer[uCount][j + 1]
        val n = edgeNormal(a, b, d)
        quad(a, b, c, d, n, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f)
    }
    for (i in 0 until uCount) {
        val a = inner[i + 1][0]
        val b = inner[i][0]
        val c = outer[i][0]
        val d = outer[i + 1][0]
        val n = edgeNormal(a, b, d)
        quad(a, b, c, d, n, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f)
    }
    for (i in 0 until uCount) {
        val a = inner[i][vCount]
        val b = inner[i + 1][vCount]
        val c = outer[i + 1][vCount]
        val d = outer[i][vCount]
        val n = edgeNormal(a, b, d)
        quad(a, b, c, d, n, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f)
    }

    for (i in 0 until uCount) {
        for (j in 0 until vCount) {
            val a = outer[i + 1][j]
            val b = outer[i][j]
            val c = outer[i][j + 1]
            val d = outer[i + 1][j + 1]
            quad(a, b, c, d, outward(a), edgeUv, 0f, edgeUv, 0f, edgeUv, 0f, edgeUv, 0f)
        }
    }

    return HomeSpacePaneMesh(verts.toFloatArray(), verts.size / HomeSpacePaneMesh.STRIDE)
}
