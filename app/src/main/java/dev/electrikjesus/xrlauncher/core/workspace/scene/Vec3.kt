package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/** BumpDesk-style 3D vector for Home Space physics and rendering. */
data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
) {
    operator fun plus(v: Vec3) = Vec3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3) = Vec3(x - v.x, y - v.y, z - v.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    fun length() = sqrt(x * x + y * y + z * z)
    fun lengthSq() = x * x + y * y + z * z

    fun dot(v: Vec3) = x * v.x + y * v.y + z * v.z

    fun yawDegrees(): Float = Math.toDegrees(atan2(x, -z).toDouble()).toFloat()

    fun pitchDegrees(): Float {
        val len = length()
        if (len <= 1e-6f) return 0f
        return Math.toDegrees(asin((y / len).toDouble())).toFloat()
    }
}
