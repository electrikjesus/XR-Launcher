package dev.electrikjesus.xrlauncher.core.launcher

import kotlinx.coroutines.flow.StateFlow

/** Shared All Apps page index for the glasses overlay + companion controls. */
object AllAppsPaginationState {
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
