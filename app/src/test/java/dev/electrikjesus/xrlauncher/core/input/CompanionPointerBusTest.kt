package dev.electrikjesus.xrlauncher.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionPointerBusTest {
    @Before
    fun reset() {
        CompanionPointerBus.resetCursor()
    }

    @Test
    fun orbitCamera_clampsPitchAndYaw() {
        CompanionPointerBus.orbitCamera(deltaYaw = 100f, deltaPitch = 100f)
        val state = CompanionPointerBus.camera.value
        assertEquals(45f, state.yawDegrees)
        assertEquals(30f, state.pitchDegrees)
    }

    @Test
    fun setFocusedPanel_neverNegative() {
        CompanionPointerBus.setFocusedPanel(-3)
        assertEquals(0, CompanionPointerBus.focusedPanelIndex.value)
    }

    @Test
    fun moveEvent_updatesNormalizedCursor() {
        CompanionPointerBus.setCursorPosition(0f, 0f)
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.MOVE, deltaX = 100f, deltaY = 50f))
        val cursor = CompanionPointerBus.cursor.value
        assertTrue(cursor.x > 0f)
        assertTrue(cursor.y > 0f)
    }

    @Test
    fun downAndUp_togglesPressedState() {
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.DOWN))
        assertTrue(CompanionPointerBus.cursor.value.isPressed)
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.UP))
        assertEquals(false, CompanionPointerBus.cursor.value.isPressed)
    }

    @Test
    fun click_keepsCursorPosition() {
        CompanionPointerBus.setCursorPosition(0.25f, 0.75f)
        CompanionPointerBus.click(PointerButton.LEFT)
        val cursor = CompanionPointerBus.cursor.value
        assertEquals(0.25f, cursor.x)
        assertEquals(0.75f, cursor.y)
        assertEquals(false, cursor.isPressed)
    }

    @Test
    fun setMotionControlEnabled_updatesState() {
        CompanionPointerBus.setMotionControlEnabled(true)
        assertEquals(true, CompanionPointerBus.motionControlEnabled.value)
        CompanionPointerBus.setMotionControlEnabled(false)
        assertEquals(false, CompanionPointerBus.motionControlEnabled.value)
    }
}
