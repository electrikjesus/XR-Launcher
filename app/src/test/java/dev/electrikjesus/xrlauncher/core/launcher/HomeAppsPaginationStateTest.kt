package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HomeAppsPaginationStateTest {
    @Before
    fun reset() {
        HomeAppsPaginationState.reset()
        AllAppsPaginationState.reset()
    }

    @Test
    fun homeAndAllAppsPages_areIndependent() {
        HomeAppsPaginationState.updatePageCount(appCount = 30, pageSize = 10)
        AllAppsPaginationState.updatePageCount(appCount = 40, pageSize = 12)
        HomeAppsPaginationState.nextPage()
        AllAppsPaginationState.goToPage(2)
        assertEquals(1, HomeAppsPaginationState.pageIndex)
        assertEquals(2, AllAppsPaginationState.pageIndex)
        HomeAppsPaginationState.prevPage()
        assertEquals(0, HomeAppsPaginationState.pageIndex)
        assertEquals(2, AllAppsPaginationState.pageIndex)
    }

    @Test
    fun paginationStateForPane_routesHomeClicks() {
        HomeAppsPaginationState.updatePageCount(20, 10)
        paginationStateForPane("home").nextPage()
        assertEquals(1, HomeAppsPaginationState.pageIndex)
        paginationStateForPane("all_apps").updatePageCount(25, 12)
        paginationStateForPane(null).nextPage()
        assertEquals(1, AllAppsPaginationState.pageIndex)
        assertEquals(1, HomeAppsPaginationState.pageIndex)
    }
}
