package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

/** Placed Desktop icons + live drag from the All Apps widget onto empty sphere space. */
object HomeSpaceDeskState {
    private const val DRAG_SLOP = 0.018f
    private const val ANGLE_SLOP_DEG = 3.5f

    data class Drag(
        val app: HomeSpaceDesk.AppRef,
        val fromDrawer: Boolean,
        val startX: Float,
        val startY: Float,
        val startYawDeg: Float,
        val startPitchDeg: Float,
        val yawDeg: Float,
        val pitchDeg: Float,
        val pulling: Boolean,
    )

    private val _placed = MutableStateFlow<List<HomeSpaceDesk.Placed>>(emptyList())
    val placedFlow: StateFlow<List<HomeSpaceDesk.Placed>> = _placed.asStateFlow()
    val placed: List<HomeSpaceDesk.Placed> get() = _placed.value

    private val _drag = MutableStateFlow<Drag?>(null)
    val dragFlow: StateFlow<Drag?> = _drag.asStateFlow()
    val drag: Drag? get() = _drag.value

    fun press(icon: HomeSpaceDesk.Icon, cursorX: Float, cursorY: Float) {
        if (!icon.isDesktopApp) {
            _drag.value = null
            return
        }
        _drag.value = Drag(
            app = icon.app,
            fromDrawer = icon.lift > 0f,
            startX = cursorX,
            startY = cursorY,
            startYawDeg = icon.yawDeg,
            startPitchDeg = icon.pitchDeg,
            yawDeg = icon.yawDeg,
            pitchDeg = icon.pitchDeg,
            pulling = false,
        )
    }

    fun move(cursorX: Float, cursorY: Float, yawDeg: Float, pitchDeg: Float) {
        val current = _drag.value ?: return
        val pulled = current.pulling ||
            hypot(cursorX - current.startX, cursorY - current.startY) > DRAG_SLOP ||
            hypot(yawDeg - current.startYawDeg, pitchDeg - current.startPitchDeg) > ANGLE_SLOP_DEG
        _drag.value = current.copy(
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            pulling = pulled,
        )
    }

    /** @return true if a drag was consumed (no click). */
    fun release(onDesktop: Boolean): Boolean {
        val current = _drag.value ?: return false
        _drag.value = null
        if (!current.pulling) return false
        if (onDesktop) {
            val next = _placed.value.filter { it.app.componentKey != current.app.componentKey } +
                HomeSpaceDesk.Placed(current.app, current.yawDeg, current.pitchDeg)
            _placed.value = next
        }
        return true
    }

    fun cancel() {
        _drag.value = null
    }

    fun clear() {
        _placed.value = emptyList()
        _drag.value = null
    }
}
