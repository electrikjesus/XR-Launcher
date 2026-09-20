package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
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
    fun encodeDecode_widget_roundTrip() {
        val layout = DeskLayout(
            items = listOf(
                DeskPlacedItem(
                    componentKey = "widget_42",
                    label = "Clock",
                    packageName = "com.android.deskclock",
                    yawDeg = -20f,
                    pitchDeg = 5f,
                    kind = "WIDGET",
                    halfWidth = 0.8f,
                    halfHeight = 0.4f,
                ),
            ),
        )
        val decoded = DeskJson.decode(DeskJson.encode(layout))
        assertEquals(1, decoded.items.size)
        val item = decoded.items.first()
        assertEquals("widget_42", item.componentKey)
        assertEquals("WIDGET", item.kind)
        assertEquals(0.8f, item.halfWidth!!, 0.01f)
        assertEquals(0.4f, item.halfHeight!!, 0.01f)
        assertEquals(-20f, item.yawDeg, 0.01f)
    }

    @Test
    fun decode_widgetKey_infersKind() {
        val decoded = DeskJson.decode("_;_|widget_7~W~pkg~-10.0000~2.0000")
        assertEquals("WIDGET", decoded.items.first().kind)
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
    fun pruneMissing_keepsWidgets() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem(
                        componentKey = "widget_9",
                        label = "W",
                        packageName = "pkg",
                        yawDeg = -5f,
                        pitchDeg = 0f,
                        kind = "WIDGET",
                        halfWidth = 0.7f,
                        halfHeight = 0.35f,
                    ),
                    DeskPlacedItem("gone/.Main", "Gone", "gone", -8f, 0f),
                ),
            ),
        )
        assertTrue(HomeSpaceDeskState.pruneMissing(emptySet()))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals("widget_9", HomeSpaceDeskState.placed.first().app.componentKey)
        assertEquals(HomeSpaceDesk.Kind.WIDGET, HomeSpaceDeskState.placed.first().app.kind)
    }

    @Test
    fun placeWidget_persistsHalfExtents() {
        HomeSpaceDeskState.placeWidget(
            appWidgetId = 3,
            label = "Calendar",
            packageName = "com.android.calendar",
            yawDeg = -15f,
            pitchDeg = 4f,
            halfWidth = 0.9f,
            halfHeight = 0.5f,
        )
        val out = HomeSpaceDeskState.toLayout()
        assertEquals(1, out.items.size)
        assertEquals("widget_3", out.items.first().componentKey)
        assertEquals("WIDGET", out.items.first().kind)
        assertEquals(0.9f, out.items.first().halfWidth!!, 0.01f)
        assertEquals(0.5f, out.items.first().halfHeight!!, 0.01f)
    }

    @Test
    fun scaleWidgets_growAndShrink() {
        HomeSpaceDeskState.placeWidget(
            appWidgetId = 5,
            label = "Clock",
            packageName = "pkg",
            yawDeg = 0f,
            pitchDeg = 0f,
            halfWidth = 0.8f,
            halfHeight = 0.4f,
        )
        assertTrue(HomeSpaceDeskState.scaleWidgets(setOf("widget_5"), DeskWidgetUtils.SIZE_STEP))
        val grown = HomeSpaceDeskState.placed.first()
        assertEquals(0.8f * DeskWidgetUtils.SIZE_STEP, grown.halfWidth!!, 0.01f)
        assertEquals(0.4f * DeskWidgetUtils.SIZE_STEP, grown.halfHeight!!, 0.01f)
        assertTrue(HomeSpaceDeskState.scaleWidgets(setOf("widget_5"), 1f / DeskWidgetUtils.SIZE_STEP))
        val shrunk = HomeSpaceDeskState.placed.first()
        assertEquals(0.8f, shrunk.halfWidth!!, 0.01f)
        assertEquals(0.4f, shrunk.halfHeight!!, 0.01f)
    }
}

class DeskWidgetUtilsTest {
    @Test
    fun widgetKey_and_parseWidgetId() {
        assertEquals("widget_12", DeskWidgetUtils.widgetKey(12))
        assertEquals(12, DeskWidgetUtils.parseWidgetId("widget_12"))
        assertNull(DeskWidgetUtils.parseWidgetId("a/.Main"))
        assertNull(DeskWidgetUtils.parseWidgetId("widget_"))
    }

    @Test
    fun scaledHalfExtents_clampsAtBounds() {
        val tiny = DeskWidgetUtils.scaledHalfExtents(0.28f, 0.12f, 1f / DeskWidgetUtils.SIZE_STEP)
        assertNull(tiny)
        val huge = DeskWidgetUtils.scaledHalfExtents(2.2f, 1.8f, DeskWidgetUtils.SIZE_STEP)
        assertNull(huge)
        val mid = DeskWidgetUtils.scaledHalfExtents(0.8f, 0.4f, DeskWidgetUtils.SIZE_STEP)!!
        assertEquals(0.8f * DeskWidgetUtils.SIZE_STEP, mid.first, 0.01f)
        assertEquals(0.4f * DeskWidgetUtils.SIZE_STEP, mid.second, 0.01f)
    }
}
