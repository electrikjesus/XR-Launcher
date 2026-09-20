package dev.electrikjesus.xrlauncher.core.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostGlassesAttachLogicTest {
    @Test
    fun shouldPrompt_whenHostHasNewSecondaryDisplay() {
        assertTrue(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = true,
                secondaryDisplayIds = listOf(7),
                dismissedDisplayIds = emptySet(),
                externalWorkspaceActive = false,
            ),
        )
    }

    @Test
    fun shouldPrompt_falseWhenNotHost() {
        assertFalse(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = false,
                secondaryDisplayIds = listOf(7),
                dismissedDisplayIds = emptySet(),
                externalWorkspaceActive = false,
            ),
        )
    }

    @Test
    fun shouldPrompt_falseWhenAlreadyDismissed() {
        assertFalse(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = true,
                secondaryDisplayIds = listOf(7),
                dismissedDisplayIds = setOf(7),
                externalWorkspaceActive = false,
            ),
        )
    }

    @Test
    fun shouldPrompt_falseWhenExternalWorkspaceOpen() {
        assertFalse(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = true,
                secondaryDisplayIds = listOf(7),
                dismissedDisplayIds = emptySet(),
                externalWorkspaceActive = true,
            ),
        )
    }

    @Test
    fun shouldPrompt_whenNewDisplayAppearsAlongsideDismissed() {
        assertTrue(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = true,
                secondaryDisplayIds = listOf(7, 9),
                dismissedDisplayIds = setOf(7),
                externalWorkspaceActive = false,
            ),
        )
    }

    @Test
    fun shouldPrompt_whenRayNeoUsbWithoutSecondaryDisplay() {
        assertTrue(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = true,
                secondaryDisplayIds = emptyList(),
                dismissedDisplayIds = emptySet(),
                externalWorkspaceActive = false,
                rayNeoUsbAttached = true,
            ),
        )
    }

    @Test
    fun shouldPrompt_falseWhenUsbOnlyAlreadyDismissed() {
        assertFalse(
            HostGlassesAttachLogic.shouldPrompt(
                hostImmersive = true,
                secondaryDisplayIds = emptyList(),
                dismissedDisplayIds = setOf(HostGlassesAttachLogic.USB_ONLY_DISMISS_ID),
                externalWorkspaceActive = false,
                rayNeoUsbAttached = true,
            ),
        )
    }

    @Test
    fun xrGlassesUiAvailable_requiresSecondary() {
        assertFalse(HostGlassesAttachLogic.xrGlassesUiAvailable(emptyList()))
        assertTrue(HostGlassesAttachLogic.xrGlassesUiAvailable(listOf(4)))
    }

    @Test
    fun pruneDismissed_clearsWhenNoSecondaryAndNoUsb() {
        assertEquals(
            emptySet<Int>(),
            HostGlassesAttachLogic.pruneDismissed(setOf(7, 9), emptyList(), rayNeoUsbAttached = false),
        )
    }

    @Test
    fun pruneDismissed_keepsLiveIdsOnly() {
        assertEquals(
            setOf(7),
            HostGlassesAttachLogic.pruneDismissed(setOf(7, 9), listOf(7)),
        )
    }

    @Test
    fun dismissIds_usbOnlyUsesSentinel() {
        assertEquals(
            setOf(HostGlassesAttachLogic.USB_ONLY_DISMISS_ID),
            HostGlassesAttachLogic.dismissIds(emptyList(), rayNeoUsbAttached = true),
        )
    }
}
