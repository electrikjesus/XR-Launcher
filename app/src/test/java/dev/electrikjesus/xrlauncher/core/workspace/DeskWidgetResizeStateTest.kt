package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeskWidgetResizeStateTest {
    @Before
    fun reset() {
        DeskWidgetResizeState.clear()
        HomeSpaceDeskState.clear()
    }

    @Test
    fun resizeFromOppositeCorner_keepsOppositeFixedIndependently() {
        val result = DeskWidgetResizeState.resizeFromOppositeCorner(
            oppositeYawDeg = -10f,
            oppositePitchDeg = -5f,
            cursorYawDeg = 10f,
            cursorPitchDeg = 5f,
            sphereScale = 1f,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
            snapToGrid = false,
        )
        // Center between opposite and cursor.
        assertEquals(0f, result.yawDeg, 0.2f)
        assertEquals(0f, result.pitchDeg, 0.2f)
        assertTrue(result.halfWidth > 0.05f)
        assertTrue(result.halfHeight > 0.02f)
        // Independent axes: wider yaw span than pitch span.
        assertTrue(result.halfWidth > result.halfHeight)
    }

    @Test
    fun resizeFromOppositeCorner_snapUsesCells() {
        val cell = DeskGrid.cellHalfExtent(0.1f, 1f)
        val result = DeskWidgetResizeState.resizeFromOppositeCorner(
            oppositeYawDeg = 0f,
            oppositePitchDeg = 0f,
            cursorYawDeg = 30f,
            cursorPitchDeg = 20f,
            sphereScale = 1f,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
            snapToGrid = true,
        )
        val (cw, ch) = DeskGrid.cellsForExtents(
            result.halfWidth,
            result.halfHeight,
            0.1f,
            1f,
        )
        assertTrue(cw >= 1)
        assertTrue(ch >= 1)
        assertEquals(cw * cell, result.halfWidth, 0.02f)
        assertEquals(ch * cell, result.halfHeight, 0.02f)
    }

    @Test
    fun parseHandle_roundTrips() {
        val key = DeskWidgetResizeState.handleKey("widget_9", DeskWidgetResizeState.Corner.NE)
        val parsed = DeskWidgetResizeState.parseHandle(key)
        assertEquals("widget_9", parsed!!.first)
        assertEquals(DeskWidgetResizeState.Corner.NE, parsed.second)
    }
}
