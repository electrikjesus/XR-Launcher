package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GlassesHomeLookTest {
    @Before
    fun reset() {
        GlassesHomeLook.reset()
        GlassesLookMode.preference = GlassesLookMode.GRADIENT
        GlassesSessionState.markLauncherForeground()
    }

    @Test
    fun homeSpaceSlots_arePinnedWidgetsHomeAndTray_notAnAllAppsPane() {
        val ids = GlassesHomeLook.homeSpaceSlots().map { it.panelId }
        assertEquals(listOf("home", "tray"), ids)
        assertFalse(ids.contains("all_apps"))
        assertFalse(ids.contains(HomeSpaceDesk.PANEL_ID))
    }

    @Test
    fun lookAtAllAppsDrawer_usesYawDegreesWhenProvided() {
        GlassesHomeLook.lastPaneArcDegrees = 72f
        GlassesHomeLook.lookAtAllAppsDrawer(yawDeg = -48f, pitchDeg = 4f)
        assertEquals(-48f, GlassesHomeLook.lookYawDegrees, 0.001f)
        assertEquals(4f, GlassesHomeLook.lookPitch, 0.001f)
        assertEquals(-48f / 72f, GlassesHomeLook.panNorm, 0.001f)
    }

    @Test
    fun lookAtAllAppsDrawer_fallsBackToDesktopPane() {
        GlassesHomeLook.lookAtAllAppsDrawer(yawDeg = null)
        assertEquals(GlassesHomeLook.PANE_LEFT, GlassesHomeLook.panNorm, 0.001f)
        assertEquals(0f, GlassesHomeLook.lookPitch, 0.001f)
    }

    @Test
    fun offCenter_pansBeforeTheOldEdgeZone() {
        GlassesHomeLook.tickEdgePan(cursorX = 0.35f, deltaSeconds = 0.2f)
        assertTrue(GlassesHomeLook.panNorm < 0f)
    }

    @Test
    fun leftEdge_pansTowardAllApps() {
        repeat(40) {
            GlassesHomeLook.tickEdgePan(cursorX = 0.02f, deltaSeconds = 0.016f)
        }
        assertTrue(GlassesHomeLook.panNorm < 0f)
        assertTrue(GlassesHomeLook.lookingAtAllApps() || GlassesHomeLook.panNorm < -0.2f)
    }

    @Test
    fun rightEdge_pansTowardTray() {
        repeat(80) {
            GlassesHomeLook.tickEdgePan(cursorX = 0.98f, deltaSeconds = 0.016f)
        }
        assertTrue(GlassesHomeLook.panNorm > 0.5f)
        assertTrue(GlassesHomeLook.lookingAtTray())
    }

    @Test
    fun pan_allowsFullCirclePastPaneBounds() {
        GlassesHomeLook.lookAt(-40f)
        assertEquals(-40f, GlassesHomeLook.panNorm, 0.001f)
        GlassesHomeLook.lookAt(40f)
        assertEquals(40f, GlassesHomeLook.panNorm, 0.001f)
    }

    @Test
    fun pan_allowsLookingLeftOfDesktopForEmptySpace() {
        assertTrue(GlassesHomeLook.minPan() < GlassesHomeLook.PANE_LEFT)
        GlassesHomeLook.lookAt(GlassesHomeLook.PANE_LEFT - 0.8f)
        assertEquals(GlassesHomeLook.PANE_LEFT - 0.8f, GlassesHomeLook.panNorm, 0.001f)
        assertTrue(GlassesHomeLook.lookingAtDesktop())
    }

    @Test
    fun pan_allowsLookingRightOfTrayForEmptySpace() {
        assertTrue(GlassesHomeLook.maxPan() > GlassesHomeLook.trayPane())
        GlassesHomeLook.lookAt(GlassesHomeLook.trayPane() + 0.8f)
        assertEquals(GlassesHomeLook.trayPane() + 0.8f, GlassesHomeLook.panNorm, 0.001f)
        assertTrue(GlassesHomeLook.lookingAtTray())
    }

    @Test
    fun leftEdge_canPanPastDesktopCenter() {
        GlassesHomeLook.lookAt(GlassesHomeLook.PANE_LEFT)
        repeat(120) {
            GlassesHomeLook.tickEdgePan(cursorX = 0.02f, deltaSeconds = 0.016f)
        }
        assertTrue(
            "edge pan should reach empty space left of Desktop",
            GlassesHomeLook.panNorm < GlassesHomeLook.PANE_LEFT - 0.2f,
        )
    }

    @Test
    fun rightEdge_canPanPastTrayCenter() {
        GlassesHomeLook.lookAt(GlassesHomeLook.trayPane())
        repeat(120) {
            GlassesHomeLook.tickEdgePan(cursorX = 0.98f, deltaSeconds = 0.016f)
        }
        assertTrue(
            "edge pan should reach empty space right of Tray",
            GlassesHomeLook.panNorm > GlassesHomeLook.trayPane() + 0.2f,
        )
    }

    @Test
    fun lookAtHome_clearsDesktopFlag() {
        GlassesHomeLook.lookAt(GlassesHomeLook.PANE_LEFT)
        assertTrue(GlassesHomeLook.lookingAtDesktop())
        GlassesHomeLook.lookAt(-0.4f)
        assertFalse(GlassesHomeLook.lookingAtDesktop())
        GlassesHomeLook.lookAt(GlassesHomeLook.PANE_HOME)
        assertFalse(GlassesHomeLook.lookingAtDesktop())
    }

    @Test
    fun openAppPlane_movesPreviousHomeToTheLeft() {
        GlassesHomeLook.lookAt(GlassesHomeLook.PANE_HOME)
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_one", componentKey = "one/.Main", label = "One"),
        )
        assertEquals(1f, GlassesHomeLook.panNorm, 0.001f)
        assertTrue(GlassesHomeLook.panNorm > GlassesHomeLook.PANE_HOME)
        assertEquals("app_one", GlassesHomeLook.focusedAppPlane()?.panelId)
        assertEquals(2f, GlassesHomeLook.trayPane(), 0.001f)
    }

    @Test
    fun openSecondAppPlane_stacksToTheRightOfTheFirst() {
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_one", componentKey = "one/.Main", label = "One"),
        )
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_two", componentKey = "two/.Main", label = "Two"),
        )
        assertEquals(2f, GlassesHomeLook.panNorm, 0.001f)
        assertEquals("app_two", GlassesHomeLook.focusedAppPlane()?.panelId)
        assertEquals(3f, GlassesHomeLook.trayPane(), 0.001f)
        assertEquals(3f + GlassesHomeLook.SIDE_LOOK_EXTRA, GlassesHomeLook.maxPan(), 0.001f)
    }

    @Test
    fun reopenSameApp_looksAtExistingPlane() {
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_one", componentKey = "one/.Main", label = "One"),
        )
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_two", componentKey = "two/.Main", label = "Two"),
        )
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_one", componentKey = "one/.Main", label = "One"),
        )
        assertEquals(1f, GlassesHomeLook.panNorm, 0.001f)
        assertEquals(2, GlassesHomeLook.appPlanes.size)
    }

    @Test
    fun closeAppPlane_returnsToPreviousPlane() {
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_one", componentKey = "one/.Main", label = "One"),
        )
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_two", componentKey = "two/.Main", label = "Two"),
        )
        GlassesHomeLook.closeAppPlane("app_two")
        assertEquals("app_one", GlassesHomeLook.focusedAppPlane()?.panelId)
        GlassesHomeLook.closeAppPlane("app_one")
        assertNull(GlassesHomeLook.focusedAppPlane())
        assertEquals(GlassesHomeLook.PANE_HOME, GlassesHomeLook.panNorm, 0.001f)
    }

    @Test
    fun lookHome_keepsAppPlanes() {
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(panelId = "app_one", componentKey = "one/.Main", label = "One"),
        )
        GlassesHomeLook.lookHome()
        assertEquals(GlassesHomeLook.PANE_HOME, GlassesHomeLook.panNorm, 0.001f)
        assertEquals(1, GlassesHomeLook.appPlanes.size)
    }

    @Test
    fun paneToTheRight_tiltsInwardTowardTheViewer() {
        val delta = GlassesHomeLook.paneDelta(worldX = 1f, look = 0f)
        assertEquals(1f, delta, 0.001f)
        assertTrue(GlassesHomeLook.paneRotationY(delta) > 40f)
        assertTrue(GlassesHomeLook.paneScale(delta) < 1f)
        assertTrue(GlassesHomeLook.paneVisible(delta))
    }

    @Test
    fun focusedPane_facesTheViewer() {
        val delta = GlassesHomeLook.paneDelta(worldX = 0f, look = 0f)
        assertEquals(0f, GlassesHomeLook.paneRotationY(delta), 0.001f)
        assertEquals(1f, GlassesHomeLook.paneScale(delta), 0.001f)
        assertEquals(1f, GlassesHomeLook.paneAlpha(delta), 0.001f)
    }

    @Test
    fun fpsLook_skipsEdgePan() {
        GlassesLookMode.preference = GlassesLookMode.FPS
        GlassesHomeLook.tickEdgePan(cursorX = 0.02f, deltaSeconds = 1f)
        assertEquals(0f, GlassesHomeLook.panNorm, 0.001f)
    }

    @Test
    fun gestureLook_skipsEdgePan() {
        GlassesLookMode.preference = GlassesLookMode.GESTURE
        GlassesHomeLook.tickEdgePan(cursorX = 0.02f, deltaSeconds = 1f)
        assertEquals(0f, GlassesHomeLook.panNorm, 0.001f)
    }

    @Test
    fun addFpsLook_turnsYawAndPitch() {
        GlassesHomeLook.addFpsLook(deltaX = 0.2f, deltaY = 0.1f)
        assertTrue(GlassesHomeLook.panNorm > 0f)
        // Finger/cursor down (positive deltaY) looks down (positive pitch).
        assertTrue(GlassesHomeLook.lookPitch > 0f)
    }
}
