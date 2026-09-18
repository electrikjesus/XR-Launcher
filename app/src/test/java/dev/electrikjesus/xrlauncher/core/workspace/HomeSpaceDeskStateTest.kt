package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeSpaceDeskStateTest {
    private val app = HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a")

    @Before
    fun reset() {
        HomeSpaceDeskState.clear()
    }

    @Test
    fun clickWithoutPull_doesNotPlace() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.505f, 0.5f, yawDeg = -40f, pitchDeg = 0f)
        assertFalse(HomeSpaceDeskState.release(onDesktop = true))
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun pullOntoEmptyDesktop_placesTheIcon() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals(app.componentKey, HomeSpaceDeskState.placed.first().app.componentKey)
        assertEquals(-12f, HomeSpaceDeskState.placed.first().yawDeg, 0.01f)
    }

    @Test
    fun pullOntoTheWidget_snapsBack() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = false))
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }
}
