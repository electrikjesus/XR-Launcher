package dev.electrikjesus.xrlauncher.core.workspace

import android.content.ComponentName
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceJsonTest {
    @Test
    fun defaultPanels_includesDrawerWidgetsHotseatAndEmptySlot() {
        val panels = Workspace.defaultPanels()
        assertEquals(5, panels.size)
        assertTrue(panels.any { it.id == "widget_calendar" && it.kind == PanelKind.WIDGET })
        assertTrue(panels.any { it.id == "empty_slot" && it.kind == PanelKind.EMPTY_SLOT && !it.visible })
    }

    @Test
    fun encodeDecode_roundTrip() {
        val workspace = Workspace(
            id = "default",
            hotseatPins = listOf("com.android.settings", "com.android.vending"),
            panels = Workspace.defaultPanels(),
            focusedPanelIndex = 1,
        )
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(workspace))
        assertEquals(workspace.id, decoded.id)
        assertEquals(workspace.hotseatPins, decoded.hotseatPins)
        assertEquals(workspace.panels.size, decoded.panels.size)
        assertEquals(1, decoded.focusedPanelIndex)
    }

    @Test
    fun encodeDecode_preservesFocusedPanelIndex() {
        val workspace = Workspace(focusedPanelIndex = 2)
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(workspace))
        assertEquals(2, decoded.focusedPanelIndex)
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
