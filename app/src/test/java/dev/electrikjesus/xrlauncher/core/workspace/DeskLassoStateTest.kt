package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeskLassoStateTest {
    @Before
    fun reset() {
        DeskLassoState.reset()
        HomeSpaceDeskState.clear()
    }

    @Test
    fun pointInPolygon_capturesInteriorIcon() {
        val square = listOf(
            DeskLassoState.Point(-20f, -10f),
            DeskLassoState.Point(20f, -10f),
            DeskLassoState.Point(20f, 10f),
            DeskLassoState.Point(-20f, 10f),
        )
        assertTrue(DeskLassoState.isInside(0f, 0f, square))
        assertFalse(DeskLassoState.isInside(40f, 0f, square))
    }

    @Test
    fun finish_selectsDesktopAppsInsideLasso() {
        val inside = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a"),
            yawDeg = 0f,
            pitchDeg = 0f,
            sphereScale = 1f,
        )
        val outside = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef("b/.Main", "Beta", "b"),
            yawDeg = 80f,
            pitchDeg = 0f,
            sphereScale = 1f,
        )
        DeskLassoState.begin(-20f, -10f)
        DeskLassoState.extend(20f, -10f)
        DeskLassoState.extend(20f, 10f)
        DeskLassoState.extend(-20f, 10f)
        assertTrue(DeskLassoState.finish(listOf(inside, outside)))
        assertEquals(setOf("a/.Main"), DeskLassoState.selectedKeys)
    }

    @Test
    fun finish_tinyStrokeDoesNotSelect() {
        DeskLassoState.begin(0f, 0f)
        DeskLassoState.extend(0.5f, 0.5f)
        assertFalse(DeskLassoState.finish(emptyList()))
        assertTrue(DeskLassoState.selectedKeys.isEmpty())
    }

    @Test
    fun completePending_returnsCapturedKeys() {
        val inside = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a"),
            yawDeg = 0f,
            pitchDeg = 0f,
            sphereScale = 1f,
        )
        DeskLassoState.begin(-20f, -10f)
        DeskLassoState.extend(20f, -10f)
        DeskLassoState.extend(20f, 10f)
        DeskLassoState.extend(-20f, 10f)
        assertTrue(DeskLassoState.notePointerUp())
        val captured = DeskLassoState.completePending(listOf(inside))
        assertEquals(setOf("a/.Main"), captured)
        assertEquals(setOf("a/.Main"), DeskLassoState.selectedKeys)
        assertFalse(DeskLassoState.active)
    }
}
