package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * BumpDesk-inspired sphere-surface physics for Desktop icons.
 * Yaw/pitch act like floor XZ; pinned panels/backing are immovable colliders.
 */
object DeskPhysics {
    const val FRICTION = 0.94f
    const val RESTITUTION = 0.28f
    const val WALL_BOUNCE = 0.35f
    /** Degrees/sec^2 of soft "settle" toward zero pitch bias — unused; keep for tuning. */
    const val MAX_SPEED_DEG = 90f
    /** Cap how far one collision may shove a body per pair resolve (avoids sphere orbit). */
    private const val MAX_SEPARATION_DEG = 12f

    data class Body(
        val key: String,
        var yawDeg: Float,
        var pitchDeg: Float,
        var velYawDeg: Float,
        var velPitchDeg: Float,
        val halfYawDeg: Float,
        val halfPitchDeg: Float,
        val mass: Float,
        val pinned: Boolean,
    )

    fun massFor(halfWidth: Float, halfHeight: Float): Float {
        val s = (halfWidth + halfHeight) * 0.5f
        return (s * s * 40f).coerceIn(0.35f, 4f)
    }

    /** Shortest signed yaw delta in (-180, 180]. */
    fun shortestYawDelta(fromDeg: Float, toDeg: Float): Float {
        var d = fromDeg - toDeg
        while (d > 180f) d -= 360f
        while (d <= -180f) d += 360f
        return d
    }

    fun step(
        bodies: MutableList<Body>,
        dtSec: Float,
        manipulatedKey: String? = null,
    ) {
        val dt = dtSec.coerceIn(0f, 0.05f)
        if (dt <= 0f || bodies.isEmpty()) return
        val manipulated = manipulatedKey?.let { key -> bodies.firstOrNull { it.key == key } }

        bodies.forEach { body ->
            if (body.pinned || body === manipulated) return@forEach
            body.yawDeg += body.velYawDeg * dt
            body.pitchDeg += body.velPitchDeg * dt
            body.velYawDeg *= FRICTION
            body.velPitchDeg *= FRICTION
            if (abs(body.velYawDeg) < 0.15f) body.velYawDeg = 0f
            if (abs(body.velPitchDeg) < 0.15f) body.velPitchDeg = 0f
            body.velYawDeg = body.velYawDeg.coerceIn(-MAX_SPEED_DEG, MAX_SPEED_DEG)
            body.velPitchDeg = body.velPitchDeg.coerceIn(-MAX_SPEED_DEG, MAX_SPEED_DEG)
            body.pitchDeg = body.pitchDeg.coerceIn(-55f, 55f)
        }

        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                resolve(bodies[i], bodies[j], manipulated)
            }
        }

        // Kill runaway slides after collision resolution (e.g. deep pane embed).
        bodies.forEach { body ->
            if (body.pinned || body === manipulated) return@forEach
            if (abs(body.velYawDeg) >= MAX_SPEED_DEG * 0.98f) {
                body.velYawDeg = 0f
                body.velPitchDeg = 0f
            }
        }
    }

    private fun resolve(a: Body, b: Body, manipulated: Body?) {
        val aCanMove = !a.pinned && a !== manipulated
        val bCanMove = !b.pinned && b !== manipulated
        if (!aCanMove && !bCanMove) return

        val dy = shortestYawDelta(a.yawDeg, b.yawDeg)
        val dp = a.pitchDeg - b.pitchDeg
        val minYaw = a.halfYawDeg + b.halfYawDeg
        val minPitch = a.halfPitchDeg + b.halfPitchDeg
        if (abs(dy) >= minYaw || abs(dp) >= minPitch) return

        // Separating axis: push along the axis with more overlap pressure.
        val overlapYaw = minYaw - abs(dy)
        val overlapPitch = minPitch - abs(dp)
        val useYaw = overlapYaw * minPitch <= overlapPitch * minYaw
        val nx: Float
        val ny: Float
        val overlap: Float
        if (useYaw) {
            // Identical centers: stable direction from keys so we don't flip every frame.
            nx = when {
                dy > 0f -> 1f
                dy < 0f -> -1f
                else -> if (a.key >= b.key) 1f else -1f
            }
            ny = 0f
            overlap = overlapYaw.coerceAtMost(MAX_SEPARATION_DEG)
        } else {
            nx = 0f
            ny = when {
                dp > 0f -> 1f
                dp < 0f -> -1f
                else -> if (a.key >= b.key) 1f else -1f
            }
            overlap = overlapPitch.coerceAtMost(MAX_SEPARATION_DEG)
        }

        val totalMass = a.mass + b.mass
        when {
            aCanMove && !bCanMove -> {
                a.yawDeg += nx * overlap
                a.pitchDeg += ny * overlap
            }
            !aCanMove && bCanMove -> {
                b.yawDeg -= nx * overlap
                b.pitchDeg -= ny * overlap
            }
            else -> {
                val aRatio = b.mass / totalMass
                val bRatio = a.mass / totalMass
                a.yawDeg += nx * overlap * aRatio
                a.pitchDeg += ny * overlap * aRatio
                b.yawDeg -= nx * overlap * bRatio
                b.pitchDeg -= ny * overlap * bRatio
            }
        }

        val relYaw = a.velYawDeg - b.velYawDeg
        val relPitch = a.velPitchDeg - b.velPitchDeg
        val velAlong = relYaw * nx + relPitch * ny
        if (velAlong >= 0f) {
            // At rest but still overlapping a pinned body — do not invent velocity.
            if (aCanMove && b.pinned) {
                a.velYawDeg = 0f
                a.velPitchDeg = 0f
            }
            if (bCanMove && a.pinned) {
                b.velYawDeg = 0f
                b.velPitchDeg = 0f
            }
            return
        }
        val invMass = (if (aCanMove) 1f / a.mass else 0f) + (if (bCanMove) 1f / b.mass else 0f)
        if (invMass <= 1e-6f) return
        val j = -(1f + RESTITUTION) * velAlong / invMass
        if (aCanMove) {
            a.velYawDeg += nx * (j / a.mass)
            a.velPitchDeg += ny * (j / a.mass)
        }
        if (bCanMove) {
            b.velYawDeg -= nx * (j / b.mass)
            b.velPitchDeg -= ny * (j / b.mass)
        }
        // Soft bounce off pinned structures.
        if (aCanMove && b.pinned) {
            a.velYawDeg *= WALL_BOUNCE
            a.velPitchDeg *= WALL_BOUNCE
        }
        if (bCanMove && a.pinned) {
            b.velYawDeg *= WALL_BOUNCE
            b.velPitchDeg *= WALL_BOUNCE
        }
    }

    fun impulseFromDrag(
        fromYaw: Float,
        fromPitch: Float,
        toYaw: Float,
        toPitch: Float,
    ): Pair<Float, Float> {
        // Soft carry — large impulses made icons jump on release.
        val dy = (toYaw - fromYaw).coerceIn(-8f, 8f)
        val dp = (toPitch - fromPitch).coerceIn(-8f, 8f)
        return dy * 0.35f to dp * 0.35f
    }

    fun separationDistance(a: Body, b: Body): Float {
        val dy = a.yawDeg - b.yawDeg
        val dp = a.pitchDeg - b.pitchDeg
        return sqrt(dy * dy + dp * dp)
    }
}
