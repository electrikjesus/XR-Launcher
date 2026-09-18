package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceCylinderGridTest {
    @Test
    fun slotCenter_placesWidgetRowHalves() {
        val clock = WorkspaceCylinderGrid.slotCenter(
            WorkspaceCylinderGrid.Slot(col = 0, row = 0, colSpan = 3, rowSpan = 1),
        )
        assertEquals(0.25f, clock.centerX, 0.001f)
        assertEquals(0.125f, clock.centerY, 0.001f)
        assertEquals(0.5f, clock.widthNorm, 0.001f)
    }

    @Test
    fun snapBounds_alignsToGridCells() {
        val snapped = WorkspaceCylinderGrid.snapBounds(
            PanelBounds(x = 0.13f, y = 0.09f, width = 0.28f, height = 0.22f),
        )
        assertEquals(WorkspaceCylinderGrid.cellWidth, snapped.x, 0.001f)
        assertEquals(0f, snapped.y, 0.001f)
        assertEquals(WorkspaceCylinderGrid.cellWidth * 2f, snapped.width, 0.001f)
        assertTrue(snapped.height >= WorkspaceCylinderGrid.cellHeight)
    }

    @Test
    fun migrateStackToFreeformBounds_assignsAllVisiblePanels() {
        val migrated = WorkspaceCylinderGrid.migrateStackToFreeformBounds(Workspace.defaultPanels())
        migrated.filter { it.visible && it.kind != PanelKind.EMPTY_SLOT }.forEach { panel ->
            assertTrue(panel.bounds != null)
        }
    }
}
