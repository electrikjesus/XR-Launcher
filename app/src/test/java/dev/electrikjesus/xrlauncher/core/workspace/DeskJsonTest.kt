package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeskJsonTest {
    @Test
    fun encodeDecode_roundTrip() {
        val layout = DeskLayout(
            items = listOf(
                DeskPlacedItem("a/.Main", "Alpha", "a", -12.5f, 4f),
                DeskPlacedItem("b/.Main", "Beta", "b", -8f, -2f),
            ),
            drawerYawDeg = -40f,
            drawerPitchDeg = 3f,
        )
        val decoded = DeskJson.decode(DeskJson.encode(layout))
        assertEquals(2, decoded.items.size)
        assertEquals("a/.Main", decoded.items[0].componentKey)
        assertEquals(-12.5f, decoded.items[0].yawDeg, 0.01f)
        assertEquals(-40f, decoded.drawerYawDeg!!, 0.01f)
        assertEquals(3f, decoded.drawerPitchDeg!!, 0.01f)
    }

    @Test
    fun encodeDecode_nullDrawer() {
        val layout = DeskLayout(items = emptyList())
        val decoded = DeskJson.decode(DeskJson.encode(layout))
        assertNull(decoded.drawerYawDeg)
        assertNull(decoded.drawerPitchDeg)
        assertTrue(decoded.items.isEmpty())
    }

    @Test
    fun decode_blank_isEmpty() {
        assertEquals(DeskLayout(), DeskJson.decode(""))
    }
}

class HomeSpaceDeskPersistTest {
    @Before
    fun reset() {
        HomeSpaceDeskState.clear()
    }

    @Test
    fun restoreAndToLayout_roundTrip() {
        val layout = DeskLayout(
            items = listOf(
                DeskPlacedItem("a/.Main", "Alpha", "a", -12f, 6f),
            ),
            drawerYawDeg = -55f,
            drawerPitchDeg = 2f,
        )
        HomeSpaceDeskState.restore(layout)
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals(-12f, HomeSpaceDeskState.placed.first().yawDeg, 0.01f)
        assertEquals(-55f, HomeSpaceDeskState.drawerPose!!.first, 0.01f)
        val out = HomeSpaceDeskState.toLayout()
        assertEquals(layout.items.first().componentKey, out.items.first().componentKey)
        assertEquals(-55f, out.drawerYawDeg!!, 0.01f)
    }

    @Test
    fun pruneMissing_dropsUninstalled() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem("a/.Main", "Alpha", "a", -12f, 0f),
                    DeskPlacedItem("gone/.Main", "Gone", "gone", -8f, 0f),
                ),
            ),
        )
        assertTrue(HomeSpaceDeskState.pruneMissing(setOf("a/.Main")))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals("a/.Main", HomeSpaceDeskState.placed.first().app.componentKey)
    }
}
