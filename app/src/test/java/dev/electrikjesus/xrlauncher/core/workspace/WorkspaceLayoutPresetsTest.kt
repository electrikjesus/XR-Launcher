package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLayoutPresetsTest {
    @Test
    fun standard_clearsBounds() {
        val withBounds = Workspace.defaultPanels().map {
            it.copy(bounds = PanelBounds(0.1f, 0.1f, 0.5f, 0.5f))
        }
        val result = WorkspaceLayoutPresets.apply(withBounds, LayoutPreset.STANDARD)
        assertTrue(result.all { it.bounds == null })
    }

    @Test
    fun single_assignsBoundsToPanels() {
        val result = WorkspaceLayoutPresets.apply(Workspace.defaultPanels(), LayoutPreset.SINGLE)
        assertNotNull(result.first { it.id == "app_drawer" }.bounds)
        assertTrue(WorkspaceLayoutPresets.usesFreeformLayout(result))
    }

    @Test
    fun inferPreset_returnsStandardWhenNoBounds() {
        assertEquals(LayoutPreset.STANDARD, WorkspaceLayoutPresets.inferPreset(Workspace.defaultPanels()))
    }

    @Test
    fun inferPreset_matchesSinglePreset() {
        val panels = WorkspaceLayoutPresets.apply(Workspace.defaultPanels(), LayoutPreset.SINGLE)
        assertEquals(LayoutPreset.SINGLE, WorkspaceLayoutPresets.inferPreset(panels))
    }
}

class WorkspaceJsonBoundsTest {
    @Test
    fun encodeDecode_preservesPanelBounds() {
        val panels = WorkspaceLayoutPresets.apply(Workspace.defaultPanels(), LayoutPreset.DUAL)
        val workspace = Workspace(panels = panels)
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(workspace))
        val drawer = decoded.panels.first { it.id == "app_drawer" }
        assertNotNull(drawer.bounds)
        assertEquals(
            panels.first { it.id == "app_drawer" }.bounds,
            drawer.bounds,
        )
    }

    @Test
    fun decode_legacyFormatWithoutPanels_usesDefaults() {
        val decoded = WorkspaceJson.decode("default|com.android.settings")
        assertNull(decoded.panels.first { it.id == "app_drawer" }.bounds)
    }
}
