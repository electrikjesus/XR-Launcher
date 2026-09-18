package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSpaceDeskTest {
    private val apps = listOf(
        HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a"),
        HomeSpaceDesk.AppRef("b/.Main", "Beta", "b"),
        HomeSpaceDesk.AppRef("c/.Main", "Gamma", "c"),
    )

    @Test
    fun lookingLeft_pitchesTheCameraOntoTheDesk() {
        val home = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val desk = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        assertEquals(0f, home.pitchDeg, 0.2f)
        assertEquals(HomeSpaceDesk.LOOK_PITCH_DEGREES, desk.pitchDeg, 0.2f)
        assertTrue(desk.yawDeg < -20f)
    }

    @Test
    fun lookingLeft_centerRayHitsTheDeskPlane() {
        val camera = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceDesk.planeHit(camera, 0.5f, 0.5f, 1920f, 1080f)
        assertNotNull(hit)
        assertEquals(HomeSpaceDesk.HEIGHT, hit!!.y, 0.04f)
        assertTrue(HomeSpaceDesk.containsHit(hit, 1f, 1920f, 1080f))
    }

    @Test
    fun layout_placesIconsOnTheDeskFloor() {
        val icons = HomeSpaceDesk.layout(apps, 1f, 1920f, 1080f)
        assertEquals(3, icons.size)
        icons.forEach { icon ->
            assertEquals(HomeSpaceDesk.HEIGHT + HomeSpaceDesk.ICON_HALF_Y, icon.center.y, 0.02f)
        }
        assertTrue(icons[1].center.x != icons[0].center.x || icons[1].center.z != icons[0].center.z)
    }

    @Test
    fun pickIcon_hitsTheIconUnderTheRay() {
        val icons = HomeSpaceDesk.layout(apps, 1f, 1920f, 1080f)
        val first = icons.first()
        val picked = HomeSpaceDesk.pickIcon(first.center, icons)
        assertEquals(first.componentKey, picked!!.componentKey)
        assertNull(HomeSpaceDesk.pickIcon(Vec3(20f, HomeSpaceDesk.HEIGHT, 20f), icons))
    }

    @Test
    fun largerSphere_movesTheDeskFartherFromTheCamera() {
        val near = HomeSpaceDesk.origin(1f, -40f)
        val far = HomeSpaceDesk.origin(1.6f, -40f)
        assertTrue(far.length() > near.length() + 0.4f)
    }
}
