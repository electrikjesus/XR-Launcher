package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchChooserTest {
    @Test
    fun withoutGlasses_onlyPhoneIsEnabled() {
        val choices = AppLaunchChooser.choices(hasSecondaryDisplay = false)
        assertEquals(AppLaunchTarget.PHONE, choices[0].target)
        assertTrue(choices[0].enabled)
        assertFalse(choices.first { it.target == AppLaunchTarget.XR_EMBEDDED }.enabled)
        assertFalse(choices.first { it.target == AppLaunchTarget.XR_FULLSCREEN }.enabled)
    }

    @Test
    fun withGlasses_allTargetsEnabled() {
        val choices = AppLaunchChooser.choices(hasSecondaryDisplay = true)
        assertTrue(choices.all { it.enabled })
    }

    @Test
    fun xrTargets_openCompanionTouchpad() {
        assertFalse(AppLaunchChooser.opensCompanionTouchpad(AppLaunchTarget.PHONE))
        assertTrue(AppLaunchChooser.opensCompanionTouchpad(AppLaunchTarget.XR_EMBEDDED))
        assertTrue(AppLaunchChooser.opensCompanionTouchpad(AppLaunchTarget.XR_FULLSCREEN))
    }
}
