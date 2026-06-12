package dev.electrikjesus.xrlauncher.core.workspace

import android.content.ComponentName
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceJsonTest {
    @Test
    fun defaultAppearance_usesReadableUiScale() {
        assertEquals(1.5f, WorkspaceAppearance.default().uiScale, 0.001f)
    }

    @Test
    fun defaultAppearance_usesSeventyPercentWorkspaceHeight() {
        assertEquals(0.7f, WorkspaceAppearance.default().workspaceHeight, 0.001f)
    }

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
    fun encodeDecode_preservesAppearance() {
        val workspace = Workspace(
            appearance = WorkspaceAppearance(
                uiScale = 1.25f,
                panelGapDp = 16f,
                wrapCurvature = 0.5f,
                workspaceWidth = 1.1f,
                workspaceHeight = 0.9f,
                lookYawDegrees = 12f,
                lookPitchDegrees = -4f,
            ),
        )
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(workspace))
        assertEquals(1.25f, decoded.appearance.uiScale, 0.001f)
        assertEquals(16f, decoded.appearance.panelGapDp, 0.001f)
        assertEquals(0.5f, decoded.appearance.wrapCurvature, 0.001f)
        assertEquals(1.1f, decoded.appearance.workspaceWidth, 0.001f)
        assertEquals(0.9f, decoded.appearance.workspaceHeight, 0.001f)
        assertEquals(12f, decoded.appearance.lookYawDegrees, 0.001f)
        assertEquals(-4f, decoded.appearance.lookPitchDegrees, 0.001f)
    }

    @Test
    fun decodeAppearance_backwardCompatibleWithLegacyTwoFieldFormat() {
        val appearance = WorkspaceJson.decode("default||widget_clock~WIDGET~true~0~0~0.5~0.25,widget_calendar~WIDGET~true~0.5~0~0.5~0.25,app_drawer~APP_DRAWER~true~0~0.25~1~0.55,hotseat~HOTSEAT~true~0~0.8~1~0.2,empty_slot~EMPTY_SLOT~false~0~0~0~0|0|1.2~8")
        assertEquals(1.2f, appearance.appearance.uiScale, 0.001f)
        assertEquals(8f, appearance.appearance.panelGapDp, 0.001f)
        assertEquals(WorkspaceAppearance.DEFAULT_WRAP_CURVATURE, appearance.appearance.wrapCurvature, 0.001f)
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
