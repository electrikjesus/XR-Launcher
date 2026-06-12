package dev.electrikjesus.xrlauncher.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionPointerBusTest {
    @Before
    fun reset() {
        CompanionPointerBus.resetCursor()
        CompanionPointerBus.setMotionControlEnabled(false)
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
    fun clickAt_setsPositionBeforeClick() {
        CompanionPointerBus.clickAt(0.1f, 0.9f, PointerButton.LEFT)
        val cursor = CompanionPointerBus.cursor.value
        assertEquals(0.1f, cursor.x)
        assertEquals(0.9f, cursor.y)
    }

    @Test
    fun recenterCursor_movesToCenter() {
        CompanionPointerBus.setCursorPosition(0.1f, 0.9f)
        CompanionPointerBus.recenterCursor()
        val cursor = CompanionPointerBus.cursor.value
        assertEquals(0.5f, cursor.x)
        assertEquals(0.5f, cursor.y)
    }

    @Test
    fun setMotionControlEnabled_updatesState() {
        CompanionPointerBus.setMotionControlEnabled(true)
        assertEquals(true, CompanionPointerBus.motionControlEnabled.value)
        CompanionPointerBus.setMotionControlEnabled(false)
        assertEquals(false, CompanionPointerBus.motionControlEnabled.value)
    }

    @Test
    fun setMotionSensitivity_clampsRange() {
        CompanionPointerBus.setMotionSensitivity(10f)
        assertEquals(3f, CompanionPointerBus.motionSensitivity.value)
        CompanionPointerBus.setMotionSensitivity(0f)
        assertEquals(0.25f, CompanionPointerBus.motionSensitivity.value)
    }

    @Test
    fun setTextEntryActive_suppressesTouchpadClick() {
        CompanionPointerBus.setTextEntryActive(true)
        assertTrue(CompanionPointerBus.isTouchpadClickSuppressed())
        CompanionPointerBus.setTextEntryActive(false)
        assertEquals(false, CompanionPointerBus.isTouchpadClickSuppressed())
    }

    @Test
    fun setManualPrecisionPointer_suppressesTouchpadClick() {
        CompanionPointerBus.setManualPrecisionPointer(true)
        assertTrue(CompanionPointerBus.isTouchpadClickSuppressed())
        CompanionPointerBus.setManualPrecisionPointer(false)
        assertEquals(false, CompanionPointerBus.isTouchpadClickSuppressed())
    }

    @Test
    fun setTouchpadSensitivity_clampsRange() {
        CompanionPointerBus.setTouchpadSensitivity(10f)
        assertEquals(3f, CompanionPointerBus.touchpadSensitivity.value)
        CompanionPointerBus.setTouchpadSensitivity(0f)
        assertEquals(0.25f, CompanionPointerBus.touchpadSensitivity.value)
    }

    @Test
    fun touchpadSensitivity_scalesMoveEvents() {
        CompanionPointerBus.setCursorPosition(0f, 0f)
        CompanionPointerBus.setTouchpadSensitivity(2f)
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.MOVE, deltaX = 100f, deltaY = 0f))
        val scaledX = CompanionPointerBus.cursor.value.x
        CompanionPointerBus.setTouchpadSensitivity(1f)
        CompanionPointerBus.setCursorPosition(0f, 0f)
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.MOVE, deltaX = 100f, deltaY = 0f))
        val baseX = CompanionPointerBus.cursor.value.x
        assertTrue(scaledX > baseX)
    }
}
