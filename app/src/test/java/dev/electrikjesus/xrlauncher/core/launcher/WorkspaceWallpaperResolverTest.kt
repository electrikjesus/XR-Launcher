package dev.electrikjesus.xrlauncher.core.launcher

import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkspaceWallpaperResolverTest {
    @Test
    fun wallpaperChoice_hasFourPresets() {
        assertEquals(4, WorkspaceWallpaperChoice.entries.size)
    }

    @Test
    fun wallpaperChoice_defaultsToSystem() {
        assertEquals(WorkspaceWallpaperChoice.SYSTEM, WorkspaceWallpaperChoice.fromPersisted(null))
    }
}
