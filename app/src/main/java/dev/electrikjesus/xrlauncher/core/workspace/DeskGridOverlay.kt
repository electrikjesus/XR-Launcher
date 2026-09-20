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
        val snapToGrid: Boolean = true,
        val showGridOnMove: Boolean = true,
        /** Live snap cell under the dragged item (null when not dragging with snap). */
        val snapTarget: DeskGrid.SnapTarget? = null,
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
        val cur = _config.value
        if (cur.visible || cur.snapTarget != null) {
            _config.value = cur.copy(visible = false, snapTarget = null)
            DeskIconTextureBus.requestRender()
        }
    }

    fun clearSnapTarget() {
        val cur = _config.value
        if (cur.snapTarget != null) {
            _config.value = cur.copy(snapTarget = null)
            DeskIconTextureBus.requestRender()
        }
    }
}
