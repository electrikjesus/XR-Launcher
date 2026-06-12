package dev.electrikjesus.xrlauncher.core.capability

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityLogicTest {
    @Test
    fun resolveFormFactor_expandedAt600dp() {
        assertEquals(LayoutFormFactor.EXPANDED, CapabilityLogic.resolveFormFactor(600))
        assertEquals(LayoutFormFactor.EXPANDED, CapabilityLogic.resolveFormFactor(800))
    }

    @Test
    fun resolveFormFactor_compactBelow600dp() {
        assertEquals(LayoutFormFactor.COMPACT, CapabilityLogic.resolveFormFactor(599))
        assertEquals(LayoutFormFactor.COMPACT, CapabilityLogic.resolveFormFactor(411))
    }

    @Test
    fun resolveTier_spatialDesktopOnExpandedWithoutGlasses() {
        assertEquals(
            RuntimeTier.SPATIAL_DESKTOP,
            CapabilityLogic.resolveTier(
                formFactor = LayoutFormFactor.EXPANDED,
                hasSecondaryDisplay = false,
                hasSpatialApi = false,
                isProjectedGlassesConnected = false,
            ),
        )
    }

    @Test
    fun resolveTier_phoneShellOnCompactWithoutDisplay() {
        assertEquals(
            RuntimeTier.PHONE_SHELL,
            CapabilityLogic.resolveTier(
                formFactor = LayoutFormFactor.COMPACT,
                hasSecondaryDisplay = false,
                hasSpatialApi = false,
                isProjectedGlassesConnected = false,
            ),
        )
    }

    @Test
    fun resolveTier_projectedGlassesWhenConnected() {
        assertEquals(
            RuntimeTier.PROJECTED_GLASSES,
            CapabilityLogic.resolveTier(
                formFactor = LayoutFormFactor.COMPACT,
                hasSecondaryDisplay = true,
                hasSpatialApi = false,
                isProjectedGlassesConnected = true,
            ),
        )
    }

    @Test
    fun resolveTier_fullSpatialWhenConnectedWithApi() {
        assertEquals(
            RuntimeTier.FULL_SPATIAL,
            CapabilityLogic.resolveTier(
                formFactor = LayoutFormFactor.COMPACT,
                hasSecondaryDisplay = true,
                hasSpatialApi = true,
                isProjectedGlassesConnected = true,
            ),
        )
    }

    @Test
    fun resolveTier_externalDisplayOnPhoneWithSecondaryDisplay() {
        assertEquals(
            RuntimeTier.EXTERNAL_DISPLAY,
            CapabilityLogic.resolveTier(
                formFactor = LayoutFormFactor.COMPACT,
                hasSecondaryDisplay = true,
                hasSpatialApi = false,
                isProjectedGlassesConnected = false,
            ),
        )
    }
}
