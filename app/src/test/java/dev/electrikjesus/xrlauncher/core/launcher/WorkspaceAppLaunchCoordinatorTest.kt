package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceAppLaunchCoordinatorTest {
    @Test
    fun parseComponentKey_relativeClassName() {
        val parsed = WorkspaceAppLaunchCoordinator.parseComponentKey("com.example/.MainActivity")
        assertEquals("com.example" to "com.example.MainActivity", parsed)
    }

    @Test
    fun parseComponentKey_fullyQualifiedClassName() {
        val parsed = WorkspaceAppLaunchCoordinator.parseComponentKey(
            "com.example/com.example.detail.DetailActivity",
        )
        assertEquals("com.example" to "com.example.detail.DetailActivity", parsed)
    }

    @Test
    fun parseComponentKey_invalid_returnsNull() {
        assertNull(WorkspaceAppLaunchCoordinator.parseComponentKey("invalid"))
        assertNull(WorkspaceAppLaunchCoordinator.parseComponentKey(""))
    }
}
