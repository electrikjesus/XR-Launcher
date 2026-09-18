package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class PerspectiveCursorProbeTest {
    @Test
    fun spiral_startsAtCenterAndSweepsThreeTurnsToTheEdges() {
        val samples = PerspectiveCursorProbe.spiralSamples(turns = 3, steps = 216)
        assertEquals(216, samples.size)
        val first = samples.first()
        assertTrue(abs(first.first - 0.5f) < 0.02f)
        assertTrue(abs(first.second - 0.5f) < 0.02f)
        val last = samples.last()
        val edgeDist = hypot(last.first - 0.5f, last.second - 0.5f)
        assertTrue("last sample should reach a display corner, dist=$edgeDist", edgeDist > 0.45f)
        val mid = samples[samples.size / 2]
        val midDist = hypot(mid.first - 0.5f, mid.second - 0.5f)
        assertTrue(midDist > 0.2f)
        assertTrue(midDist < edgeDist)
    }

    @Test
    fun smallCircle_staysInsideTheDisplay() {
        val samples = PerspectiveCursorProbe.smallCircleSamples(32)
        assertEquals(32, samples.size)
        samples.forEach { (x, y) ->
            assertTrue(x in 0.25f..0.75f)
            assertTrue(y in 0.20f..0.80f)
        }
        val xs = samples.map { it.first }
        val ys = samples.map { it.second }
        assertTrue(xs.max() - xs.min() > 0.12f)
        assertTrue(ys.max() - ys.min() > 0.20f)
    }

    @Test
    fun cornerCircle_hitsAllFourDisplayCorners() {
        val samples = PerspectiveCursorProbe.cornerCircleSamples(96)
        val corners = listOf(
            0f to 0f,
            1f to 0f,
            1f to 1f,
            0f to 1f,
        )
        corners.forEach { (cx, cy) ->
            val nearest = samples.minOf { (x, y) -> hypot(x - cx, y - cy) }
            assertTrue("expected a sample near ($cx, $cy), nearest=$nearest", nearest < 0.08f)
        }
    }

    @Test
    fun cornerAngles_areFourDistinctDirections() {
        val angles = PerspectiveCursorProbe.cornerAngles()
        assertEquals(4, angles.size)
        for (i in angles.indices) {
            for (j in i + 1 until angles.size) {
                assertTrue(abs(angles[i] - angles[j]) > 0.4f)
            }
        }
    }
}
