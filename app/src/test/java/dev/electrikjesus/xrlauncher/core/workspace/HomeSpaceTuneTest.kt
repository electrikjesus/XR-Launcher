package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSpaceTuneTest {
    @Test
    fun apply_nudgesPanelSphereAndElementScales() {
        val base = WorkspaceAppearance.default()
        val panel = HomeSpaceTune.apply(base, HomeSpaceTuneAxis.PANEL, HomeSpaceTune.STEP)
        val sphere = HomeSpaceTune.apply(base, HomeSpaceTuneAxis.SPHERE, HomeSpaceTune.STEP)
        val element = HomeSpaceTune.apply(base, HomeSpaceTuneAxis.ELEMENT, HomeSpaceTune.STEP)
        assertEquals(base.panelScale + HomeSpaceTune.STEP, panel.panelScale, 0.001f)
        assertEquals(base.sphereScale + HomeSpaceTune.STEP, sphere.sphereScale, 0.001f)
        assertEquals(base.uiScale + HomeSpaceTune.STEP, element.uiScale, 0.001f)
    }

    @Test
    fun apply_clampsToAppearanceLimits() {
        val maxed = HomeSpaceTune.apply(
            WorkspaceAppearance(sphereScale = WorkspaceAppearance.MAX_SPHERE_SCALE),
            HomeSpaceTuneAxis.SPHERE,
            1f,
        )
        assertEquals(WorkspaceAppearance.MAX_SPHERE_SCALE, maxed.sphereScale, 0.001f)
    }

    @Test
    fun apply_togglesDesktopItemKinds() {
        val base = WorkspaceAppearance.default()
        val iconsOff = HomeSpaceTune.apply(base, HomeSpaceTuneAxis.DESK_ICONS, 0f)
        assertEquals(false, iconsOff.desktopIcons)
        val pilesOff = HomeSpaceTune.apply(iconsOff, HomeSpaceTuneAxis.DESK_PILES, 0f)
        assertEquals(false, pilesOff.desktopPiles)
        assertEquals(true, pilesOff.desktopTiles)
    }
}
