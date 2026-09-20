package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LauncherContextMenuStateTest {
    @Before
    fun reset() {
        LauncherContextMenuState.dismiss()
    }

    @Test
    fun openApp_setsRequestAndIsOpen() {
        val app = LaunchableApp(
            label = "Example",
            packageName = "com.example",
            componentName = android.content.ComponentName("com.example", ".Main"),
        )
        LauncherContextMenuState.openApp(app, isPinned = false, anchorX = 0.5f, anchorY = 0.5f)

        assertTrue(LauncherContextMenuState.isOpen)
        val request = LauncherContextMenuState.request.value
        assertNotNull(request)
        assertTrue(request!!.target is LauncherContextMenuTarget.App)
        assertEquals(0.5f, request.anchorX, 0.001f)
    }

    @Test
    fun openApp_storesWorldLockPose() {
        val app = LaunchableApp(
            label = "Example",
            packageName = "com.example",
            componentName = android.content.ComponentName("com.example", ".Main"),
        )
        LauncherContextMenuState.openApp(
            app = app,
            isPinned = false,
            anchorX = 0.5f,
            anchorY = 0.5f,
            deskYawDeg = -22f,
            deskPitchDeg = 6f,
        )
        val request = LauncherContextMenuState.request.value
        assertNotNull(request)
        assertEquals(-22f, request!!.deskYawDeg!!, 0.001f)
        assertEquals(6f, request.deskPitchDeg!!, 0.001f)
    }

    @Test
    fun dismiss_clearsRequest() {
        LauncherContextMenuState.openPanel(
            panelId = "widget_clock",
            kind = PanelKind.WIDGET,
            anchorX = 0.2f,
            anchorY = 0.3f,
        )
        assertTrue(LauncherContextMenuState.isOpen)

        LauncherContextMenuState.dismiss()

        assertFalse(LauncherContextMenuState.isOpen)
        assertNull(LauncherContextMenuState.request.value)
    }
}

private fun assertEquals(expected: Float, actual: Float, delta: Float) {
    org.junit.Assert.assertEquals(expected, actual, delta)
}
