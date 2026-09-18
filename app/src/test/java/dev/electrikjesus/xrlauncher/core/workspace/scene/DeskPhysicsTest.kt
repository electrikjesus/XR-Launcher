package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    private fun abs(v: Float) = if (v < 0f) -v else v
}
