package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskArrangeTest {
    private fun placed(key: String, yaw: Float, pitch: Float) = HomeSpaceDesk.Placed(
        app = HomeSpaceDesk.AppRef(componentKey = key, label = key, packageName = key),
        yawDeg = yaw,
        pitchDeg = pitch,
    )

    @Test
    fun arrange_requiresAtLeastTwo() {
        val one = listOf(placed("a", 0f, 0f))
        assertNull(
            DeskArrange.arrange(
                placed = one,
                keys = setOf("a"),
                mode = DeskArrangeMode.ROW,
                sphereScale = 1f,
                halfWidth = HomeSpaceDesk.ICON_HALF_WIDTH,
                halfHeight = HomeSpaceDesk.ICON_HALF_HEIGHT,
            ),
        )
    }

    @Test
    fun arrange_row_spreadsYawKeepsPitch() {
        val items = listOf(
            placed("a", -10f, 5f),
            placed("b", 0f, 3f),
            placed("c", 10f, 7f),
        )
        val next = DeskArrange.arrange(
            placed = items,
            keys = setOf("a", "b", "c"),
            mode = DeskArrangeMode.ROW,
            sphereScale = 1f,
            halfWidth = HomeSpaceDesk.ICON_HALF_WIDTH,
            halfHeight = HomeSpaceDesk.ICON_HALF_HEIGHT,
        )
        assertNotNull(next)
        val byKey = next!!.associateBy { it.app.componentKey }
        val pitches = listOf(byKey.getValue("a"), byKey.getValue("b"), byKey.getValue("c"))
            .map { it.pitchDeg }
        assertEquals(pitches[0], pitches[1], 0.01f)
        assertEquals(pitches[1], pitches[2], 0.01f)
        assertTrue(byKey.getValue("a").yawDeg < byKey.getValue("b").yawDeg)
        assertTrue(byKey.getValue("b").yawDeg < byKey.getValue("c").yawDeg)
    }

    @Test
    fun arrange_column_spreadsPitchKeepsYaw() {
        val items = listOf(
            placed("a", -4f, 8f),
            placed("b", 2f, 0f),
            placed("c", 6f, -8f),
        )
        val next = DeskArrange.arrange(
            placed = items,
            keys = setOf("a", "b", "c"),
            mode = DeskArrangeMode.COLUMN,
            sphereScale = 1f,
            halfWidth = HomeSpaceDesk.ICON_HALF_WIDTH,
            halfHeight = HomeSpaceDesk.ICON_HALF_HEIGHT,
        )
        assertNotNull(next)
        val byKey = next!!.associateBy { it.app.componentKey }
        val yaws = listOf(byKey.getValue("a"), byKey.getValue("b"), byKey.getValue("c"))
            .map { it.yawDeg }
        assertEquals(yaws[0], yaws[1], 0.01f)
        assertEquals(yaws[1], yaws[2], 0.01f)
        assertTrue(byKey.getValue("a").pitchDeg > byKey.getValue("b").pitchDeg)
        assertTrue(byKey.getValue("b").pitchDeg > byKey.getValue("c").pitchDeg)
    }

    @Test
    fun arrange_grid_placesInRows() {
        val items = listOf(
            placed("a", -20f, 0f),
            placed("b", 0f, 0f),
            placed("c", 20f, 0f),
            placed("d", 40f, 0f),
        )
        val grid = DeskArrange.arrange(
            placed = items,
            keys = setOf("a", "b", "c", "d"),
            mode = DeskArrangeMode.GRID,
            sphereScale = 1f,
            halfWidth = HomeSpaceDesk.ICON_HALF_WIDTH,
            halfHeight = HomeSpaceDesk.ICON_HALF_HEIGHT,
        )!!
        val pitches = grid.map { it.pitchDeg }.distinct()
        assertTrue(pitches.size >= 2)
    }
}
