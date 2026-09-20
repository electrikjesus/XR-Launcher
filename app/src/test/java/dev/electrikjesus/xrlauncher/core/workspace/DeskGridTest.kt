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

    @Test
    fun snapTarget_matchesPoseAndFootprintCells() {
        val target = DeskGrid.snapTarget(
            yawDeg = 7f,
            pitchDeg = -4f,
            halfWidth = 0.1f,
            halfHeight = 0.125f,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
            sphereScale = 1f,
        )
        val (yaw, pitch) = DeskGrid.snapPose(7f, -4f, 0.1f, 1f, 1f)
        assertEquals(yaw, target.yawDeg, 0.01f)
        assertEquals(pitch, target.pitchDeg, 0.01f)
        assertEquals(1, target.cellsW)
        assertEquals(1, target.cellsH)
        val outline = DeskGrid.snapTargetOutline(target)
        assertTrue(outline.size >= 5)
        assertEquals(outline.first().first, outline.last().first, 0.01f)
        assertEquals(outline.first().second, outline.last().second, 0.01f)
    }

    @Test
    fun snapTarget_widgetSpansMultipleCells() {
        val cell = DeskGrid.cellHalfExtent(0.1f, 1f)
        val target = DeskGrid.snapTarget(
            yawDeg = 0f,
            pitchDeg = 0f,
            halfWidth = cell * 2f,
            halfHeight = cell,
            iconHalfWidth = 0.1f,
            gridScale = 1f,
            sphereScale = 1f,
        )
        assertEquals(2, target.cellsW)
        assertEquals(1, target.cellsH)
        assertTrue(target.halfYawDeg > target.halfPitchDeg)
    }
}
