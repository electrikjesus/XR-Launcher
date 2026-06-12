package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLayoutPresetsTest {
    @Test
    fun standard_assignsGridBounds() {
        val result = WorkspaceLayoutPresets.apply(Workspace.defaultPanels(), LayoutPreset.STANDARD)
        assertNotNull(result.first { it.id == "widget_clock" }.bounds)
        assertTrue(WorkspaceLayoutPresets.usesFreeformLayout(result))
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
    fun inferPreset_matchesStandardGridPreset() {
        val panels = WorkspaceLayoutPresets.apply(Workspace.defaultPanels(), LayoutPreset.STANDARD)
        assertEquals(LayoutPreset.STANDARD, WorkspaceLayoutPresets.inferPreset(panels))
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
        val original = panels.first { it.id == "app_drawer" }.bounds!!
        val decodedBounds = drawer.bounds!!
        assertEquals(original.x, decodedBounds.x, 0.001f)
        assertEquals(original.y, decodedBounds.y, 0.001f)
        assertEquals(original.width, decodedBounds.width, 0.001f)
        assertEquals(original.height, decodedBounds.height, 0.001f)
    }

    @Test
    fun decode_legacyFormatWithoutPanels_usesDefaults() {
        val decoded = WorkspaceJson.decode("default|com.android.settings")
        assertNull(decoded.panels.first { it.id == "app_drawer" }.bounds)
    }
}
