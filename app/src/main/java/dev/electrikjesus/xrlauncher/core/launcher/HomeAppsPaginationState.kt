package dev.electrikjesus.xrlauncher.core.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mutable page index shared by Compose grids and the glasses pointer bus. */
class AppsPageState {
    private val _pageIndex = MutableStateFlow(0)
    val pageIndexFlow: StateFlow<Int> = _pageIndex.asStateFlow()

    private val _pageCount = MutableStateFlow(1)
    val pageCountFlow: StateFlow<Int> = _pageCount.asStateFlow()

    var pageIndex: Int
        get() = _pageIndex.value
        set(value) {
            _pageIndex.value = AppDrawerPagination.clampPageIndex(value, _pageCount.value)
        }

    val pageCount: Int
        get() = _pageCount.value

    fun reset() {
        _pageIndex.value = 0
        _pageCount.value = 1
    }

    fun updatePageCount(appCount: Int, pageSize: Int) {
        _pageCount.value = AppDrawerPagination.pageCount(appCount, pageSize)
        _pageIndex.value = AppDrawerPagination.clampPageIndex(_pageIndex.value, _pageCount.value)
    }

    fun goToPage(index: Int) {
        pageIndex = index
    }

    fun nextPage() {
        goToPage(_pageIndex.value + 1)
    }

    fun prevPage() {
        goToPage(_pageIndex.value - 1)
    }
}

/** Home pane app-grid pages. Distinct from the All Apps overlay so pointer clicks switch the visible grid. */
object HomeAppsPaginationState {
    internal val pages = AppsPageState()

    val pageIndexFlow: StateFlow<Int> = pages.pageIndexFlow
    val pageCountFlow: StateFlow<Int> = pages.pageCountFlow

    var pageIndex: Int
        get() = pages.pageIndex
        set(value) {
            pages.pageIndex = value
        }

    fun reset() = pages.reset()

    fun updatePageCount(appCount: Int, pageSize: Int) = pages.updatePageCount(appCount, pageSize)

    fun goToPage(index: Int) = pages.goToPage(index)

    fun nextPage() = pages.nextPage()

    fun prevPage() = pages.prevPage()
}

fun paginationStateForPane(paneId: String?): AppsPageState = when (paneId) {
    "home" -> HomeAppsPaginationState.pages
    else -> AllAppsPaginationState.pages
}
