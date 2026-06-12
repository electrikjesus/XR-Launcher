package dev.electrikjesus.xrlauncher.core.capability

import dev.electrikjesus.xrlauncher.core.workspace.EmbedMode
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import org.junit.Assert.assertEquals
import org.junit.Test

class SpatialEmbedCapabilityTest {
    @Test
    fun preferredEmbedMode_emptySlot_isFullWindowWithoutSpatialApi() {
        val panel = PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT)
        assertEquals(
            EmbedMode.FULL_WINDOW,
            SpatialEmbedCapability.preferredEmbedMode(hasSpatialApi = false, panel),
        )
    }

    @Test
    fun preferredEmbedMode_emptySlot_isEmbeddedWithSpatialApi() {
        val panel = PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT)
        assertEquals(
            EmbedMode.EMBEDDED,
            SpatialEmbedCapability.preferredEmbedMode(hasSpatialApi = true, panel),
        )
    }

    @Test
    fun preferredEmbedMode_drawer_isNone() {
        val panel = PanelState(id = "app_drawer", kind = PanelKind.APP_DRAWER)
        assertEquals(
            EmbedMode.NONE,
            SpatialEmbedCapability.preferredEmbedMode(hasSpatialApi = true, panel),
        )
    }

    @Test
    fun launchLabel_withoutSpatialApi_isFullLaunch() {
        val panel = PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT)
        assertEquals("Full launch", SpatialEmbedCapability.launchLabel(hasSpatialApi = false, panel))
    }
}
