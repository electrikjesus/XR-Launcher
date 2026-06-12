package dev.electrikjesus.xrlauncher.core.launcher

object AppDrawerPagination {
    fun pageCount(appCount: Int, pageSize: Int): Int {
        if (appCount <= 0 || pageSize <= 0) return 1
        return (appCount + pageSize - 1) / pageSize
    }

    fun clampPageIndex(pageIndex: Int, pageCount: Int): Int =
        pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    fun pageApps(
        apps: List<LaunchableApp>,
        pageIndex: Int,
        pageSize: Int,
    ): List<LaunchableApp> {
        if (apps.isEmpty() || pageSize <= 0) return emptyList()
        val pageCount = pageCount(apps.size, pageSize)
        val safeIndex = clampPageIndex(pageIndex, pageCount)
        val start = safeIndex * pageSize
        return apps.subList(start, minOf(start + pageSize, apps.size))
    }

    /** Window of page indices to show as tappable buttons (centered on [currentPage]). */
    fun visiblePageButtons(currentPage: Int, pageCount: Int, maxButtons: Int = 7): List<Int> {
        if (pageCount <= 0) return emptyList()
        if (pageCount <= maxButtons) return (0 until pageCount).toList()
        val half = maxButtons / 2
        var start = (currentPage - half).coerceAtLeast(0)
        var end = start + maxButtons
        if (end > pageCount) {
            end = pageCount
            start = (end - maxButtons).coerceAtLeast(0)
        }
        return (start until end).toList()
    }
}
