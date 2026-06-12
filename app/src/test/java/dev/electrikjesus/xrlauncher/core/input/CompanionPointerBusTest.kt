package dev.electrikjesus.xrlauncher.core.input

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionPointerBusTest {
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
}
