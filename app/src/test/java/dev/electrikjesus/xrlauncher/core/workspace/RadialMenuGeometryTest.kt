package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class RadialMenuGeometryTest {
    @Test
    fun firstSlot_sitsAboveTheAnchor() {
        val (dx, dy) = RadialMenuGeometry.slotOffsetPx(0, 4, 100f)
        assertEquals(0f, dx, 0.01f)
        assertEquals(-100f, dy, 0.01f)
    }

    @Test
    fun fourSlots_walkClockwise() {
        val right = RadialMenuGeometry.slotOffsetPx(1, 4, 80f)
        val down = RadialMenuGeometry.slotOffsetPx(2, 4, 80f)
        val left = RadialMenuGeometry.slotOffsetPx(3, 4, 80f)
        assertEquals(80f, right.first, 0.01f)
        assertEquals(0f, right.second, 0.01f)
        assertEquals(0f, down.first, 0.01f)
        assertEquals(80f, down.second, 0.01f)
        assertEquals(-80f, left.first, 0.01f)
        assertEquals(0f, left.second, 0.01f)
    }

    @Test
    fun emptyRing_staysAtAnchor() {
        assertEquals(0f to 0f, RadialMenuGeometry.slotOffsetPx(0, 0, 40f))
    }
}
