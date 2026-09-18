package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class HomeSpaceCursorTest {
    private val slots = listOf(
        HomeSpacePaneSlot("all_apps", -1f),
        HomeSpacePaneSlot("home", 0f),
        HomeSpacePaneSlot("tray", 1f),
    )

    @Test
    fun viewPoint_roundTripsThroughWorldDirection() {
        val camera = HomeSpaceScene.Camera(yawDeg = 32f, pitchDeg = 11f)
        val world = Vec3(0.35f, 0.12f, -1.6f)
        val view = camera.viewPoint(world)
        val recovered = camera.worldDirection(view)
        assertEquals(world.x, recovered.x, 0.002f)
        assertEquals(world.y, recovered.y, 0.002f)
        assertEquals(world.z, recovered.z, 0.002f)
    }

    @Test
    fun centerCursor_hitsHomePaneNearCenter() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val pick = HomeSpaceScene.pickPane(
            cursorX = 0.5f,
            cursorY = 0.5f,
            camera = camera,
            slots = slots,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertNotNull(pick)
        assertEquals("home", pick!!.slot.panelId)
        assertEquals(0.5f, pick.u, 0.04f)
        assertEquals(0.5f, pick.v, 0.04f)
    }

    @Test
    fun paneEdgeOnTheSphere_picksNearU1() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val mesh = HomeSpaceScene.paneMesh(0f, 1920f, 1080f)
        val edge = mesh.frontVertexNear(0.92f, 0.5f)
        val projected = HomeSpaceScene.projectToView(edge, camera, 1920f, 1080f)
        val cursorX = 0.5f + projected.x / 1920f
        val cursorY = 0.5f + projected.y / 1080f
        assertTrue("projected pane edge should leave the view center", cursorX > 0.62f)
        val pick = HomeSpaceScene.pickPane(
            cursorX = cursorX,
            cursorY = cursorY,
            camera = camera,
            slots = slots,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertNotNull(pick)
        assertEquals("home", pick!!.slot.panelId)
        assertEquals(0.92f, pick.u, 0.08f)
    }

    @Test
    fun farRightRay_isNotAFlatHudPlaneAtCenter() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceScene.sphereHit(0.92f, 0.5f, camera, 1920f, 1080f)
        val radius = HomeSpaceScene.innerSphereRadius()
        assertEquals(radius, hit.world.length(), 0.03f)
        assertTrue(
            "a view-right ray must land off the screen-center of the sphere",
            abs(hit.yawDeg) > 20f,
        )
        val pick = HomeSpaceScene.pickPane(
            cursorX = 0.92f,
            cursorY = 0.5f,
            camera = camera,
            slots = slots,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(pick == null || pick.slot.panelId == "home" || pick.slot.panelId == "tray")
        if (pick?.slot?.panelId == "home") {
            assertTrue(pick.u > 0.72f)
        }
    }

    @Test
    fun lookingLeft_centerRayHitsAllApps() {
        val camera = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        val pick = HomeSpaceScene.pickPane(
            cursorX = 0.5f,
            cursorY = 0.5f,
            camera = camera,
            slots = slots,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertNotNull(pick)
        assertEquals("all_apps", pick!!.slot.panelId)
        assertEquals(0.5f, pick.u, 0.08f)
    }

    @Test
    fun overlayPx_mapsUvOntoTheCaptureCard() {
        val pick = HomeSpacePanePick(
            slot = HomeSpacePaneSlot("home", 0f),
            u = 0.25f,
            v = 0.75f,
            hit = HomeSpaceSphereHit(Vec3(0f, 0f, -1f), 0f, 0f),
        )
        val (x, y) = HomeSpaceScene.overlayPx(pick, 100f, 200f, 400f, 300f)
        assertEquals(200f, x, 0.01f)
        assertEquals(275f, y, 0.01f)
    }

    @Test
    fun overlayPx_keepsDistanceFromCenterOnASpiralSample() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val pick = HomeSpaceScene.pickPane(
            cursorX = 0.5f,
            cursorY = 0.5f,
            camera = camera,
            slots = slots,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertNotNull(pick)
        val (x, y) = HomeSpaceScene.overlayPx(pick!!, 240f, 64f, 1440f, 950f)
        assertTrue(hypot(x - 960f, y - 539f) < 80f)
    }

    @Test
    fun missBetweenPanes_returnsNullPick() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val pick = HomeSpaceScene.pickPane(
            cursorX = 0.5f,
            cursorY = 0.02f,
            camera = camera,
            slots = slots,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertNull(pick)
    }
}
