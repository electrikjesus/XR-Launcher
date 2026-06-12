package dev.electrikjesus.xrlauncher.core.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared All Apps page index for the glasses overlay + companion controls. */
object AllAppsPaginationState {
    private val _pageIndex = MutableStateFlow(0)
    val pageIndexFlow: StateFlow<Int> = _pageIndex.asStateFlow()

    private val _pageCount = MutableStateFlow(1)
    val pageCountFlow: StateFlow<Int> = _pageCount.asStateFlow()

    var pageIndex: Int
        get() = _pageIndex.value
        set(value) {
            _pageIndex.value = AppDrawerPagination.clampPageIndex(value, _pageCount.value)
        }

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
