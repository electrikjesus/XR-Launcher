package dev.electrikjesus.xrlauncher.core.workspace

import android.content.ComponentName
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceJsonTest {
    @Test
    fun encodeDecode_roundTrip() {
        val workspace = Workspace(
            id = "default",
            hotseatPins = listOf("com.android.settings", "com.android.vending"),
            panels = Workspace.defaultPanels(),
        )
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(workspace))
        assertEquals(workspace.id, decoded.id)
        assertEquals(workspace.hotseatPins, decoded.hotseatPins)
        assertEquals(workspace.panels.size, decoded.panels.size)
    }
}

class HotseatResolverTest {
    private fun app(pkg: String, cls: String, label: String) = LaunchableApp(
        label = label,
        componentName = ComponentName(pkg, cls),
        packageName = pkg,
    )

    @Test
    fun resolveHotseatApps_usesPinnedOrder() {
        val apps = listOf(
            app("com.android.settings", ".Settings", "Settings"),
            app("com.android.vending", ".AssetBrowserActivity", "Play Store"),
            app("com.example", ".Main", "Example"),
        )
        val pins = listOf("com.example", "com.android.settings")
        val hotseat = HotseatResolver.resolveHotseatApps(apps, pins, maxSlots = 3)
        assertEquals(listOf("Example", "Settings", "Play Store"), hotseat.map { it.label })
    }

    @Test
    fun resolveHotseatApps_fallsBackToDefaultsWhenEmpty() {
        val apps = listOf(
            app("com.android.settings", ".Settings", "Settings"),
            app("com.android.vending", ".AssetBrowserActivity", "Play Store"),
        )
        val hotseat = HotseatResolver.resolveHotseatApps(apps, pinnedKeys = emptyList())
        assertTrue(hotseat.any { it.packageName == "com.android.settings" })
        assertTrue(hotseat.any { it.packageName == "com.android.vending" })
    }
}
