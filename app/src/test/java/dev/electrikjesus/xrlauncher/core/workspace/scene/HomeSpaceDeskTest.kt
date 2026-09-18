package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val yaw = HomeSpaceDesk.yawDegrees(1920f, 1080f)
        assertEquals(HomeSpaceDesk.origin(1f, yaw).y, hit!!.y, 0.04f)
        assertTrue(HomeSpaceDesk.containsHit(hit, 1f, 1920f, 1080f))
    }

    @Test
    fun lookingLeft_centerRayPicksTheAllAppsTile() {
        val camera = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceDesk.planeHit(camera, 0.5f, 0.5f, 1920f, 1080f)
        assertNotNull(hit)
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        val yaw = HomeSpaceDesk.yawDegrees(1920f, 1080f)
        assertEquals(1, icons.size)
        assertTrue(icons.first().isAppDrawer)
        assertEquals(HomeSpaceDesk.DRAWER_KEY, HomeSpaceDesk.pickIcon(hit!!, icons, yaw)!!.componentKey)
    }

    @Test
    fun defaultIcons_areThinPancakes() {
        assertTrue(HomeSpaceDesk.ICON_HALF_Y * 4f < HomeSpaceDesk.ICON_HALF_X)
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        assertTrue(drawer.halfY * 4f < drawer.halfX)
    }

    @Test
    fun layout_keepsTheDrawerWhenPlacingApps() {
        val icons = HomeSpaceDesk.layout(apps, 1f, 1920f, 1080f)
        assertEquals(1 + apps.size, icons.size)
        assertTrue(icons.first().isAppDrawer)
        val floorY = HomeSpaceDesk.origin(1f, HomeSpaceDesk.yawDegrees(1920f, 1080f)).y
        icons.forEach { icon ->
            assertEquals(floorY + HomeSpaceDesk.ICON_HALF_Y, icon.center.y, 0.02f)
        }
        assertTrue(icons[1].center.x != icons[2].center.x || icons[1].center.z != icons[2].center.z)
    }

    @Test
    fun pickIcon_hitsTheIconUnderTheRay() {
        val icons = HomeSpaceDesk.layout(apps, 1f, 1920f, 1080f)
        val yaw = HomeSpaceDesk.yawDegrees(1920f, 1080f)
        val first = icons.first()
        val picked = HomeSpaceDesk.pickIcon(first.center, icons, yaw)
        assertEquals(first.componentKey, picked!!.componentKey)
        val miss = Vec3(20f, first.center.y, 20f)
        assertNull(HomeSpaceDesk.pickIcon(miss, icons, yaw))
    }

    @Test
    fun pickIcon_usesDeskLocalAxesNotWorldAabb() {
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        val yaw = HomeSpaceDesk.yawDegrees(1920f, 1080f)
        val drawer = icons.first()
        val worldXMiss = Vec3(
            drawer.center.x + drawer.halfX * 2.2f,
            drawer.center.y,
            drawer.center.z,
        )
        assertNull(HomeSpaceDesk.pickIcon(worldXMiss, icons, yaw))
        val alongRight = HomeSpaceDesk.rightAxis(yaw)
        val localHit = Vec3(
            drawer.center.x + alongRight.x * drawer.halfX * 0.4f,
            drawer.center.y,
            drawer.center.z + alongRight.z * drawer.halfX * 0.4f,
        )
        assertEquals(drawer.componentKey, HomeSpaceDesk.pickIcon(localHit, icons, yaw)!!.componentKey)
    }

    @Test
    fun lookingHome_centerRayDoesNotHitTheDesk() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceDesk.planeHit(camera, 0.5f, 0.5f, 1920f, 1080f)
        assertTrue(hit == null || !HomeSpaceDesk.containsHit(hit, 1f, 1920f, 1080f))
    }

    @Test
    fun largerSphere_movesTheDeskFartherFromTheCamera() {
        val near = HomeSpaceDesk.origin(1f, -40f)
        val far = HomeSpaceDesk.origin(1.6f, -40f)
        assertTrue(far.length() > near.length() + 0.4f)
    }

    @Test
    fun hoverPad_isLargerThanTheIcon() {
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        val yaw = HomeSpaceDesk.yawDegrees(1920f, 1080f)
        val pad = HomeSpaceDesk.hoverPadMesh(drawer, yaw)
        assertEquals(6, pad.vertexCount)
        assertEquals(-2f, pad.interleaved[6], 0.001f)
        val alongRight = HomeSpaceDesk.rightAxis(yaw)
        val locals = (0 until pad.vertexCount).map { i ->
            val p = pad.position(i)
            (p.x - drawer.center.x) * alongRight.x + (p.z - drawer.center.z) * alongRight.z
        }
        assertTrue(locals.maxOrNull()!! - locals.minOrNull()!! > drawer.halfX * 2f)
    }
}
