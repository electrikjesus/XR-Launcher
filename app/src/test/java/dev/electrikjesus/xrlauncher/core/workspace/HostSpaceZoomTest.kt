package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostSpaceZoomTest {
    @Test
    fun scrollUp_decreasesSphere_zoomIn() {
        val delta = HostSpaceZoom.sphereDeltaFromScroll(-HostSpaceZoom.SCROLL_PIXELS_PER_STEP)
        assertEquals(-HomeSpaceTune.STEP, delta, 0.0001f)
    }

    @Test
    fun scrollDown_increasesSphere_zoomOut() {
        val delta = HostSpaceZoom.sphereDeltaFromScroll(HostSpaceZoom.SCROLL_PIXELS_PER_STEP)
        assertEquals(HomeSpaceTune.STEP, delta, 0.0001f)
    }

    @Test
    fun pinchOut_decreasesSphere_zoomIn() {
        val delta = HostSpaceZoom.sphereDeltaFromPinch(100f, 120f)
        assertTrue(delta < 0f)
    }

    @Test
    fun pinchIn_increasesSphere_zoomOut() {
        val delta = HostSpaceZoom.sphereDeltaFromPinch(100f, 80f)
        assertTrue(delta > 0f)
    }

    @Test
    fun zeroInputs_noop() {
        assertEquals(0f, HostSpaceZoom.sphereDeltaFromScroll(0f), 0f)
        assertEquals(0f, HostSpaceZoom.sphereDeltaFromPinch(0f, 100f), 0f)
    }
}
