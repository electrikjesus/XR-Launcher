package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeSpaceDialogStateTest {
    @Before
    fun reset() {
        HomeSpaceDialogState.close()
    }

    @Test
    fun openSettings_setsDialog() {
        HomeSpaceDialogState.openSettings()
        assertEquals(HomeSpaceDialog.SETTINGS, HomeSpaceDialogState.dialog)
    }

    @Test
    fun toggleSettings_opensAndCloses() {
        HomeSpaceDialogState.toggleSettings()
        assertEquals(HomeSpaceDialog.SETTINGS, HomeSpaceDialogState.dialog)
        HomeSpaceDialogState.toggleSettings()
        assertEquals(HomeSpaceDialog.NONE, HomeSpaceDialogState.dialog)
    }

    @Test
    fun isDialogTexture_matchesKnownIds() {
        assertTrue(HomeSpaceDialogState.isDialogTexture(HomeSpaceDialogState.EDIT_TEXTURE_ID))
        assertTrue(HomeSpaceDialogState.isDialogTexture(HomeSpaceDialogState.SETTINGS_TEXTURE_ID))
        assertFalse(HomeSpaceDialogState.isDialogTexture("home"))
    }
}
