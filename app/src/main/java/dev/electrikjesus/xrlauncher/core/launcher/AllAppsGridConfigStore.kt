package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AllAppsGridDimensions(
    val columns: Int,
    val rows: Int,
) {
    val pageSize: Int get() = columns * rows
}

/** Persists paginated All Apps grid size from Settings. */
object AllAppsGridConfigStore {
    private const val PREFS_NAME = "launcher_settings"
    private const val KEY_COLUMNS = "all_apps_grid_columns"
    private const val KEY_ROWS = "all_apps_grid_rows"

    private var loaded = false

    private val _dimensions = MutableStateFlow(
        AllAppsGridDimensions(
            columns = AllAppsGridConfig.DEFAULT_COLUMNS,
            rows = AllAppsGridConfig.DEFAULT_ROWS,
        ),
    )
    val dimensions: StateFlow<AllAppsGridDimensions> = _dimensions.asStateFlow()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        _dimensions.value = load(context.applicationContext)
        applyToRuntime(_dimensions.value)
    }

    fun current(): AllAppsGridDimensions = _dimensions.value

    fun load(context: Context): AllAppsGridDimensions {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AllAppsGridDimensions(
            columns = prefs.getInt(KEY_COLUMNS, AllAppsGridConfig.DEFAULT_COLUMNS)
                .coerceIn(AllAppsGridConfig.MIN_COLUMNS, AllAppsGridConfig.MAX_COLUMNS),
            rows = prefs.getInt(KEY_ROWS, AllAppsGridConfig.DEFAULT_ROWS)
                .coerceIn(AllAppsGridConfig.MIN_ROWS, AllAppsGridConfig.MAX_ROWS),
        )
    }

    fun saveColumns(context: Context, columns: Int) {
        save(context, current().copy(columns = columns))
    }

    fun saveRows(context: Context, rows: Int) {
        save(context, current().copy(rows = rows))
    }

    fun resetToDefaults(context: Context) {
        save(
            context,
            AllAppsGridDimensions(
                columns = AllAppsGridConfig.DEFAULT_COLUMNS,
                rows = AllAppsGridConfig.DEFAULT_ROWS,
            ),
        )
    }

    private fun save(context: Context, dimensions: AllAppsGridDimensions) {
        val normalized = AllAppsGridDimensions(
            columns = dimensions.columns.coerceIn(AllAppsGridConfig.MIN_COLUMNS, AllAppsGridConfig.MAX_COLUMNS),
            rows = dimensions.rows.coerceIn(AllAppsGridConfig.MIN_ROWS, AllAppsGridConfig.MAX_ROWS),
        )
        _dimensions.value = normalized
        applyToRuntime(normalized)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_COLUMNS, normalized.columns)
            .putInt(KEY_ROWS, normalized.rows)
            .apply()
    }

    private fun applyToRuntime(dimensions: AllAppsGridDimensions) {
        AllAppsGridConfig.setGrid(dimensions.columns, dimensions.rows)
    }
}
