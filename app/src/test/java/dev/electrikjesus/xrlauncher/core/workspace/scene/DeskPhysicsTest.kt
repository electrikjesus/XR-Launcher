package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DeskPhysicsTest {
    @Test
    fun movableIconsPushEachOtherApart() {
        val a = DeskPhysics.Body(
            key = "a",
            yawDeg = 0f,
            pitchDeg = 0f,
            velYawDeg = 0f,
            velPitchDeg = 0f,
            halfYawDeg = 3f,
            halfPitchDeg = 3f,
            mass = 1f,
            pinned = false,
        )
        val b = DeskPhysics.Body(
            key = "b",
            yawDeg = 1f,
            pitchDeg = 0f,
            velYawDeg = 0f,
            velPitchDeg = 0f,
            halfYawDeg = 3f,
            halfPitchDeg = 3f,
            mass = 1f,
            pinned = false,
        )
        val bodies = mutableListOf(a, b)
        DeskPhysics.step(bodies, dtSec = 1f / 60f)
        assertTrue(abs(bodies[0].yawDeg - bodies[1].yawDeg) >= 5.9f)
    }

    @Test
    fun pinnedPanelDoesNotMoveWhenHit() {
        val icon = DeskPhysics.Body(
            key = "icon",
            yawDeg = 0f,
            pitchDeg = 0f,
            velYawDeg = 20f,
            velPitchDeg = 0f,
            halfYawDeg = 3f,
            halfPitchDeg = 3f,
            mass = 1f,
            pinned = false,
        )
        val pane = DeskPhysics.Body(
            key = "pane",
            yawDeg = 4f,
            pitchDeg = 0f,
            velYawDeg = 0f,
            velPitchDeg = 0f,
            halfYawDeg = 8f,
            halfPitchDeg = 10f,
            mass = 100f,
            pinned = true,
        )
        val bodies = mutableListOf(icon, pane)
        repeat(8) { DeskPhysics.step(bodies, dtSec = 1f / 60f) }
        assertEquals(4f, bodies[1].yawDeg, 0.01f)
        assertEquals(0f, bodies[1].velYawDeg, 0.01f)
        assertTrue(bodies[0].yawDeg < 4f)
    }

    @Test
    fun shortestYawDelta_wrapsAroundSphere() {
        assertEquals(20f, DeskPhysics.shortestYawDelta(170f, 150f), 0.01f)
        assertEquals(-20f, DeskPhysics.shortestYawDelta(170f, -170f), 0.01f)
        assertEquals(10f, DeskPhysics.shortestYawDelta(-175f, 175f), 0.01f)
    }

    @Test
    fun deepPinnedOverlap_doesNotOrbitForever() {
        // Resting icon buried in a wide pane — must crawl out without runaway velocity.
        val icon = DeskPhysics.Body(
            key = "icon",
            yawDeg = 0f,
            pitchDeg = 0f,
            velYawDeg = 0f,
            velPitchDeg = 0f,
            halfYawDeg = 3f,
            halfPitchDeg = 3f,
            mass = 1f,
            pinned = false,
        )
        val pane = DeskPhysics.Body(
            key = "pane",
            yawDeg = 0f,
            pitchDeg = 0f,
            velYawDeg = 0f,
            velPitchDeg = 0f,
            halfYawDeg = 25f,
            halfPitchDeg = 20f,
            mass = 100f,
            pinned = true,
        )
        val bodies = mutableListOf(icon, pane)
        var maxAbsVel = 0f
        repeat(90) {
            DeskPhysics.step(bodies, dtSec = 1f / 60f)
            maxAbsVel = maxOf(maxAbsVel, abs(bodies[0].velYawDeg), abs(bodies[0].velPitchDeg))
        }
        // Separation is capped; at rest vs pinned should not invent orbit speed.
        assertTrue(maxAbsVel < DeskPhysics.MAX_SPEED_DEG * 0.5f)
        assertEquals(0f, bodies[0].velYawDeg, 0.01f)
        assertEquals(0f, bodies[0].velPitchDeg, 0.01f)
        // Eventually clear of the pane yaw extent (centers farther than combined half).
        assertTrue(abs(bodies[0].yawDeg - bodies[1].yawDeg) >= 25f)
    }
}
