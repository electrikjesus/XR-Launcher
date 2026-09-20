package dev.electrikjesus.xrlauncher.core.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the GLES desk grid should draw (move / resize / group-move while setting on).
 * Updated from pointer + resize state; read by [dev.electrikjesus.xrlauncher.ui.spatial.gles.CylinderGlRenderer].
 */
object DeskGridOverlay {
    data class Config(
        val visible: Boolean = false,
        val centerYawDeg: Float = 0f,
        val centerPitchDeg: Float = 0f,
        val iconHalfWidth: Float = 0.162f,
        val gridScale: Float = DeskGrid.DEFAULT_GRID_SCALE,
        val sphereScale: Float = 1f,
        val snapToGrid: Boolean = false,
        val showGridOnMove: Boolean = true,
    )

    private val _config = MutableStateFlow(Config())
    val configFlow: StateFlow<Config> = _config.asStateFlow()
    val config: Config get() = _config.value

    fun update(config: Config) {
        if (_config.value == config) return
        _config.value = config
        DeskIconTextureBus.requestRender()
    }

    fun hide() {
        if (_config.value.visible) {
            _config.value = _config.value.copy(visible = false)
            DeskIconTextureBus.requestRender()
        }
    }
}
