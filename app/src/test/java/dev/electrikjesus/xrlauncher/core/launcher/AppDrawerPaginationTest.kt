package dev.electrikjesus.xrlauncher.core.launcher

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDrawerPaginationTest {
    private val apps = (1..12).map { index ->
        LaunchableApp("App $index", ComponentName("pkg$index", ".Main"), "pkg$index")
    }

    @Test
    fun pageCount_roundsUp() {
        assertEquals(1, AppDrawerPagination.pageCount(0, 25))
        assertEquals(1, AppDrawerPagination.pageCount(12, 25))
        assertEquals(2, AppDrawerPagination.pageCount(26, 25))
        assertEquals(3, AppDrawerPagination.pageCount(12, 5))
    }

    @Test
    fun pageApps_returnsSliceForPage() {
        val pageSize = 5
        assertEquals(listOf("App 1", "App 2", "App 3", "App 4", "App 5"), pageLabels(0, pageSize))
        assertEquals(listOf("App 6", "App 7", "App 8", "App 9", "App 10"), pageLabels(1, pageSize))
        assertEquals(listOf("App 11", "App 12"), pageLabels(2, pageSize))
    }

    @Test
    fun visiblePageButtons_centersOnCurrentPage() {
        assertEquals(listOf(0, 1, 2), AppDrawerPagination.visiblePageButtons(1, 3, maxButtons = 7))
        assertEquals(listOf(3, 4, 5, 6, 7), AppDrawerPagination.visiblePageButtons(5, 10, maxButtons = 5))
    }

    private fun pageLabels(pageIndex: Int, pageSize: Int): List<String> =
        AppDrawerPagination.pageApps(apps, pageIndex, pageSize).map { it.label }
}
