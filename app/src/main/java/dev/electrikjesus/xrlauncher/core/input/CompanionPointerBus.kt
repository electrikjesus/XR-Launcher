package dev.electrikjesus.xrlauncher.core.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PointerAction {
    MOVE,
    DOWN,
    UP,
}

data class PointerEvent(
    val action: PointerAction,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
)

data class WorkspaceCameraState(
    val yawDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
)

object CompanionPointerBus {
    private val _events = MutableSharedFlow<PointerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PointerEvent> = _events.asSharedFlow()

    private val _camera = MutableStateFlow(WorkspaceCameraState())
    val camera: StateFlow<WorkspaceCameraState> = _camera.asStateFlow()

    private val _focusedPanelIndex = MutableStateFlow(0)
    val focusedPanelIndex: StateFlow<Int> = _focusedPanelIndex.asStateFlow()

    fun emit(event: PointerEvent) {
        _events.tryEmit(event)
    }

    fun orbitCamera(deltaYaw: Float, deltaPitch: Float) {
        val current = _camera.value
        _camera.value = current.copy(
            yawDegrees = (current.yawDegrees + deltaYaw).coerceIn(-45f, 45f),
            pitchDegrees = (current.pitchDegrees + deltaPitch).coerceIn(-30f, 30f),
        )
    }

    fun setFocusedPanel(index: Int) {
        _focusedPanelIndex.value = index.coerceAtLeast(0)
    }
}
