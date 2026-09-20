package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeskGroupMoveStateTest {
    @Before
    fun reset() {
        DeskGroupMoveState.clear()
        HomeSpaceDeskState.clear()
    }

    @Test
    fun arm_requiresTwoKeys() {
        DeskGroupMoveState.arm(setOf("a/.Main"))
        assertNull(DeskGroupMoveState.armedKeys)
        DeskGroupMoveState.arm(setOf("a/.Main", "b/.Main"))
        assertEquals(setOf("a/.Main", "b/.Main"), DeskGroupMoveState.armedKeys)
    }

    @Test
    fun move_preservesRelativeOffsets() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem("a/.Main", "A", "a", -20f, 0f),
                    DeskPlacedItem("b/.Main", "B", "b", -10f, 5f),
                ),
            ),
        )
        DeskGroupMoveState.arm(setOf("a/.Main", "b/.Main"))
        assertTrue(
            DeskGroupMoveState.beginDrag(
                componentKey = "a/.Main",
                cursorX = 0.5f,
                cursorY = 0.5f,
                grabYawDeg = -20f,
                grabPitchDeg = 0f,
                placed = HomeSpaceDeskState.placed,
            ),
        )
        // Pull past angle slop
        val poses = DeskGroupMoveState.move(
            cursorX = 0.5f,
            cursorY = 0.5f,
            yawDeg = -5f,
            pitchDeg = 2f,
            angleSlopDeg = 1f,
        )
        assertNotNull(poses)
        assertEquals(-5f, poses!!["a/.Main"]!!.first, 0.01f)
        assertEquals(2f, poses["a/.Main"]!!.second, 0.01f)
        // b was +10 yaw and +5 pitch from a at grab
        assertEquals(5f, poses["b/.Main"]!!.first, 0.01f)
        assertEquals(7f, poses["b/.Main"]!!.second, 0.01f)
        HomeSpaceDeskState.applyGroupPoses(poses)
        assertTrue(DeskGroupMoveState.endDrag())
        assertNull(DeskGroupMoveState.armedKeys)
        assertEquals(-5f, HomeSpaceDeskState.placed.first { it.app.componentKey == "a/.Main" }.yawDeg, 0.01f)
        assertEquals(5f, HomeSpaceDeskState.placed.first { it.app.componentKey == "b/.Main" }.yawDeg, 0.01f)
    }

    @Test
    fun appendHandle_atCentroid() {
        val placed = listOf(
            HomeSpaceDesk.Placed(
                app = HomeSpaceDesk.AppRef("a/.Main", "A", "a"),
                yawDeg = -20f,
                pitchDeg = 0f,
            ),
            HomeSpaceDesk.Placed(
                app = HomeSpaceDesk.AppRef("b/.Main", "B", "b"),
                yawDeg = -10f,
                pitchDeg = 10f,
            ),
        )
        DeskGroupMoveState.arm(setOf("a/.Main", "b/.Main"))
        val icons = DeskGroupMoveState.appendHandle(emptyList(), placed, sphereScale = 1f, halfWidth = 0.1f)
        assertEquals(1, icons.size)
        assertTrue(icons.first().isGroupHandle)
        assertEquals(-15f, icons.first().yawDeg, 0.01f)
        assertEquals(5f, icons.first().pitchDeg, 0.01f)
    }

    @Test
    fun endDrag_withoutPull_keepsArmed() {
        DeskGroupMoveState.arm(setOf("a/.Main", "b/.Main"))
        assertTrue(
            DeskGroupMoveState.beginDrag(
                componentKey = DeskGroupMoveState.HANDLE_KEY,
                cursorX = 0.4f,
                cursorY = 0.4f,
                grabYawDeg = 0f,
                grabPitchDeg = 0f,
                placed = listOf(
                    HomeSpaceDesk.Placed(
                        app = HomeSpaceDesk.AppRef("a/.Main", "A", "a"),
                        yawDeg = -4f,
                        pitchDeg = 0f,
                    ),
                    HomeSpaceDesk.Placed(
                        app = HomeSpaceDesk.AppRef("b/.Main", "B", "b"),
                        yawDeg = 4f,
                        pitchDeg = 0f,
                    ),
                ),
            ),
        )
        assertFalse(DeskGroupMoveState.endDrag())
        assertEquals(setOf("a/.Main", "b/.Main"), DeskGroupMoveState.armedKeys)
    }
}
