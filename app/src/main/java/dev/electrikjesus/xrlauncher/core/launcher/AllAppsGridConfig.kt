package dev.electrikjesus.xrlauncher.core.launcher

/** Grid dimensions for paginated All Apps views (configurable in Settings later). */
object AllAppsGridConfig {
    const val DEFAULT_COLUMNS = 5
    const val DEFAULT_ROWS = 5
    const val MIN_COLUMNS = 3
    const val MAX_COLUMNS = 7
    const val MIN_ROWS = 3
    const val MAX_ROWS = 6

    var columns: Int = DEFAULT_COLUMNS
        private set
    var rows: Int = DEFAULT_ROWS
        private set

    val pageSize: Int get() = columns * rows

    fun setGrid(columns: Int, rows: Int) {
        this.columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        this.rows = rows.coerceIn(MIN_ROWS, MAX_ROWS)
    }

    fun resetToDefaults() {
        columns = DEFAULT_COLUMNS
        rows = DEFAULT_ROWS
    }
}
