package dev.electrikjesus.xrlauncher.core.input

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLookOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class CompanionPointerBusTest {
    @Before
    fun reset() {
        CompanionPointerBus.resetCursor()
        CompanionPointerBus.setMotionControlEnabled(false)
        GlassesLookMode.preference = GlassesLookMode.GRADIENT
        GlassesHomeLook.reset()
        GlassesSessionState.markLauncherForeground()
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
        assertTrue(cursor.isPressed)
    }

    @Test
    fun clickAt_setsPositionBeforeClick() {
        CompanionPointerBus.clickAt(0.1f, 0.9f, PointerButton.LEFT)
        val cursor = CompanionPointerBus.cursor.value
        assertEquals(0.1f, cursor.x)
        assertEquals(0.9f, cursor.y)
    }

    @Test
    fun clickListener_receivesLeftClickWhenLauncherForeground() {
        GlassesSessionState.markLauncherForeground()
        var received: PointerClick? = null
        val listener: (PointerClick) -> Unit = { received = it }
        CompanionPointerBus.addClickListener(listener)
        try {
            CompanionPointerBus.clickAt(0.3f, 0.4f, PointerButton.LEFT)
            assertEquals(0.3f, received?.x)
            assertEquals(0.4f, received?.y)
            assertEquals(PointerButton.LEFT, received?.button)
        } finally {
            CompanionPointerBus.removeClickListener(listener)
        }
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
    fun focusNextPanel_cyclesThroughIds() {
        val ids = listOf("widget_clock", "app_drawer", "hotseat")
        CompanionPointerBus.setFocusedPanelId("widget_clock")
        CompanionPointerBus.focusNextPanel(ids)
        assertEquals("app_drawer", CompanionPointerBus.focusedPanelId.value)
        CompanionPointerBus.focusNextPanel(ids)
        assertEquals("hotseat", CompanionPointerBus.focusedPanelId.value)
        CompanionPointerBus.focusNextPanel(ids)
        assertEquals("widget_clock", CompanionPointerBus.focusedPanelId.value)
    }

    @Test
    fun focusNextPanelIndex_cyclesThroughIndices() {
        CompanionPointerBus.setFocusedPanel(0)
        CompanionPointerBus.focusNextPanelIndex(4)
        assertEquals(1, CompanionPointerBus.focusedPanelIndex.value)
        CompanionPointerBus.focusPreviousPanelIndex(4)
        assertEquals(0, CompanionPointerBus.focusedPanelIndex.value)
        CompanionPointerBus.setFocusedPanel(0)
        CompanionPointerBus.focusPreviousPanelIndex(4)
        assertEquals(3, CompanionPointerBus.focusedPanelIndex.value)
    }

    @Test
    fun setTouchpadSensitivity_clampsRange() {
        CompanionPointerBus.setTouchpadSensitivity(10f)
        assertEquals(3f, CompanionPointerBus.touchpadSensitivity.value)
        CompanionPointerBus.setTouchpadSensitivity(0f)
        assertEquals(0.25f, CompanionPointerBus.touchpadSensitivity.value)
    }

    @Test
    fun lookBy_updatesRuntimeOffset() {
        WorkspaceLookOffset.reset()
        CompanionPointerBus.lookBy(deltaX = 100f, deltaY = 0f)
        assertTrue(WorkspaceLookOffset.yawDegrees > 0f)
        CompanionPointerBus.recenterCursor()
        assertEquals(0f, WorkspaceLookOffset.yawDegrees, 0.001f)
    }

    @Test
    fun emitMove_updatesCursorPosition() {
        CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.MOVE, deltaX = 100f, deltaY = 0f))
        assertTrue(CompanionPointerBus.cursor.value.x > 0.5f)
    }

    @Test
    fun applyGlassesImuSample_doesNotSteerLookOffsetOnLauncher() {
        GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
        GlassesSessionState.launcherForeground = true
        try {
            WorkspaceLookOffset.reset()
            CompanionPointerBus.applyGlassesImuSample(
                gyroXDps = 0f,
                gyroYDps = -30f,
                gyroZDps = 0f,
                deltaTimeSec = 0.016f,
            )
            assertEquals(0f, WorkspaceLookOffset.yawDegrees, 0.001f)
            assertEquals(0f, WorkspaceLookOffset.pitchDegrees, 0.001f)
        } finally {
            GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
            GlassesSessionState.launcherForeground = false
            CompanionPointerBus.resetCursor()
        }
    }

    @Test
    fun applyGlassesImuSample_movesCursorOnLauncherForeground() {
        GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
        GlassesSessionState.launcherForeground = true
        try {
            CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
            CompanionPointerBus.applyGlassesImuSample(
                gyroXDps = 0f,
                gyroYDps = -30f,
                gyroZDps = 0f,
                deltaTimeSec = 0.016f,
            )
            assertTrue(CompanionPointerBus.cursor.value.x > 0.5f)
        } finally {
            GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
            GlassesSessionState.launcherForeground = false
            CompanionPointerBus.resetCursor()
        }
    }

    @Test
    fun applyGlassesImuSample_movesCursorOverOtherApps() {
        GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
        GlassesSessionState.launcherForeground = false
        try {
            CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
            CompanionPointerBus.applyGlassesImuSample(
                gyroXDps = 0f,
                gyroYDps = -30f,
                gyroZDps = 0f,
                deltaTimeSec = 0.016f,
            )
            assertTrue(CompanionPointerBus.cursor.value.x > 0.5f)
        } finally {
            GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
            CompanionPointerBus.resetCursor()
        }
    }

    @Test
    fun applyGlassesImuSample_usesSeparateYawAndPitchScales() {
        GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
        GlassesSessionState.launcherForeground = false
        try {
            CompanionPointerBus.setGlassesImuYawScale(0.5f)
            CompanionPointerBus.setGlassesImuPitchScale(1.5f)
            CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
            CompanionPointerBus.applyGlassesImuSample(
                gyroXDps = -30f,
                gyroYDps = -30f,
                gyroZDps = 0f,
                deltaTimeSec = 0.016f,
            )
            val x = CompanionPointerBus.cursor.value.x
            val y = CompanionPointerBus.cursor.value.y
            assertTrue(x > 0.5f)
            assertTrue(y > 0.5f)
            assertTrue(abs(y - 0.5f) > abs(x - 0.5f))
        } finally {
            GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
            CompanionPointerBus.resetGlassesImuMovementScales()
            CompanionPointerBus.resetCursor()
        }
    }

    @Test
    fun applyGlassesImuSample_ignoredInCompanionMode() {
        GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
        CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
        CompanionPointerBus.applyGlassesImuSample(
            gyroXDps = 0f,
            gyroYDps = 30f,
            gyroZDps = 0f,
            deltaTimeSec = 0.016f,
        )
        assertEquals(0.5f, CompanionPointerBus.cursor.value.x, 0.001f)
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

    @Test
    fun moveBy_fpsLocksCursorAndTurnsTheView() {
        GlassesLookMode.preference = GlassesLookMode.FPS
        GlassesSessionState.markLauncherForeground()
        CompanionPointerBus.setCursorPosition(0.4f, 0.6f)
        GlassesHomeLook.reset()
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.MOVE, deltaX = 100f, deltaY = 0f))
        assertEquals(0.5f, CompanionPointerBus.cursor.value.x, 0.001f)
        assertEquals(0.5f, CompanionPointerBus.cursor.value.y, 0.001f)
        assertTrue(GlassesHomeLook.panNorm > 0f)
        GlassesLookMode.preference = GlassesLookMode.GRADIENT
    }

    @Test
    fun moveBy_fpsFallsBackToCursorWhenAnAppIsInFront() {
        GlassesLookMode.preference = GlassesLookMode.FPS
        GlassesSessionState.launcherForeground = false
        CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
        CompanionPointerBus.emit(PointerEvent(action = PointerAction.MOVE, deltaX = 100f, deltaY = 0f))
        assertTrue(CompanionPointerBus.cursor.value.x > 0.5f)
        GlassesLookMode.preference = GlassesLookMode.GRADIENT
        GlassesSessionState.markLauncherForeground()
    }
}
