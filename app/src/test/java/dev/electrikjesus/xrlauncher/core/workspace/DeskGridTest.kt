package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskGridTest {
    @Test
    fun snapPose_roundsToCellSteps() {
        val (yaw, pitch) = DeskGrid.snapPose(
            yawDeg = 7f,
            pitchDeg = -3f,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
            sphereScale = 1f,
        )
        val stepYaw = DeskGrid.cellYawDeg(0.1f, 1f, 1f)
        val stepPitch = DeskGrid.cellPitchDeg(0.1f, 1f, 1f)
        assertEquals(0f, yaw % stepYaw, 0.05f)
        assertEquals(0f, pitch % stepPitch, 0.05f)
    }

    @Test
    fun snapHalfExtents_usesIntegerCells() {
        val cell = DeskGrid.cellHalfExtent(0.1f, 1f)
        val snapped = DeskGrid.snapHalfExtents(
            halfWidth = cell * 2.4f,
            halfHeight = cell * 1.1f,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
        )
        val (cw, ch) = DeskGrid.cellsForExtents(snapped.first, snapped.second, 0.1f, 1f)
        assertEquals(2, cw)
        assertEquals(1, ch)
        assertEquals(cell * 2f, snapped.first, 0.001f)
        assertEquals(cell, snapped.second, 0.001f)
    }

    @Test
    fun overlayLines_includeVerticalAndHorizontal() {
        val lines = DeskGrid.overlayLines(
            centerYawDeg = 0f,
            centerPitchDeg = 0f,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
            sphereScale = 1f,
            halfSpanYawDeg = 12f,
            halfSpanPitchDeg = 8f,
            samplesPerLine = 4,
        )
        assertTrue(lines.size >= 4)
        assertTrue(lines.any { line -> line.all { it.first == line.first().first } })
        assertTrue(lines.any { line -> line.all { it.second == line.first().second } })
    }
}
