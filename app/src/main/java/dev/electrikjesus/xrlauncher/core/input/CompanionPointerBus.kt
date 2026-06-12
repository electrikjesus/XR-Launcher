package dev.electrikjesus.xrlauncher.core.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState

enum class PointerAction {
    MOVE,
    DOWN,
    UP,
}

enum class PointerButton {
    LEFT,
    RIGHT,
}

data class PointerEvent(
    val action: PointerAction,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
)

data class PointerClick(
    val button: PointerButton,
    val x: Float,
    val y: Float,
)

data class WorkspaceCameraState(
    val yawDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
)

/** Normalized cursor position (0..1) shared between phone companion and external display. */
data class CompanionCursorState(
    val x: Float = 0.5f,
    val y: Float = 0f,
    val isPressed: Boolean = false,
    val hoveredLabel: String? = null,
)

object CompanionPointerBus {
    private const val TOUCHPAD_SENSITIVITY = 0.004f
    private const val MOTION_SENSITIVITY = 0.015f

    private val _events = MutableSharedFlow<PointerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PointerEvent> = _events.asSharedFlow()

    private val _clicks = MutableSharedFlow<PointerClick>(extraBufferCapacity = 16)
    val clicks: SharedFlow<PointerClick> = _clicks.asSharedFlow()

    private val _camera = MutableStateFlow(WorkspaceCameraState())
    val camera: StateFlow<WorkspaceCameraState> = _camera.asStateFlow()

    private val _focusedPanelIndex = MutableStateFlow(0)
    val focusedPanelIndex: StateFlow<Int> = _focusedPanelIndex.asStateFlow()

    private val _cursor = MutableStateFlow(CompanionCursorState())
    val cursor: StateFlow<CompanionCursorState> = _cursor.asStateFlow()

    private val _motionControlEnabled = MutableStateFlow(false)
    val motionControlEnabled: StateFlow<Boolean> = _motionControlEnabled.asStateFlow()

    private val _motionSensitivity = MutableStateFlow(1f)
    val motionSensitivity: StateFlow<Float> = _motionSensitivity.asStateFlow()

    private val _glassesControlMode = MutableStateFlow(GlassesSessionState.controlMode)
    val glassesControlMode: StateFlow<GlassesControlMode> = _glassesControlMode.asStateFlow()

    fun emit(event: PointerEvent) {
        if (event.action == PointerAction.MOVE) {
            moveBy(event.deltaX * TOUCHPAD_SENSITIVITY, event.deltaY * TOUCHPAD_SENSITIVITY)
        } else if (event.action == PointerAction.DOWN) {
            _cursor.value = _cursor.value.copy(isPressed = true)
        } else if (event.action == PointerAction.UP) {
            _cursor.value = _cursor.value.copy(isPressed = false)
        }
        _events.tryEmit(event)
    }

    fun moveBy(deltaX: Float, deltaY: Float) {
        val current = _cursor.value
        _cursor.value = current.copy(
            x = (current.x + deltaX).coerceIn(0f, 1f),
            y = (current.y + deltaY).coerceIn(0f, 1f),
        )
    }

    fun moveByMotion(deltaX: Float, deltaY: Float) {
        val scale = MOTION_SENSITIVITY * _motionSensitivity.value
        moveBy(deltaX * scale, deltaY * scale)
    }

    fun setCursorPosition(x: Float, y: Float) {
        _cursor.value = _cursor.value.copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
        )
    }

    fun recenterCursor() {
        setCursorPosition(0.5f, 0.5f)
    }

    fun setHoveredLabel(label: String?) {
        if (_cursor.value.hoveredLabel != label) {
            _cursor.value = _cursor.value.copy(hoveredLabel = label)
        }
    }

    fun click(button: PointerButton) {
        val current = _cursor.value
        val injectOnGlasses =
            GlassesSessionState.controlMode == GlassesControlMode.DESKTOP &&
                GlassesSessionState.secondaryDisplayId != null &&
                DisplayPointerInjector.isAvailable

        if (injectOnGlasses) {
            DisplayPointerInjector.dispatchClick(
                GlassesSessionState.secondaryDisplayId!!,
                current.x,
                current.y,
                button,
            )
        } else {
            emitClick(PointerClick(button = button, x = current.x, y = current.y))
        }
        if (button == PointerButton.LEFT) {
            _cursor.value = current.copy(isPressed = true)
            _cursor.value = _cursor.value.copy(isPressed = false)
        }
    }

    fun clickAt(x: Float, y: Float, button: PointerButton) {
        setCursorPosition(x, y)
        click(button)
    }

    private fun emitClick(click: PointerClick) {
        _clicks.tryEmit(click)
    }

    fun setMotionControlEnabled(enabled: Boolean) {
        _motionControlEnabled.value = enabled
    }

    fun setMotionSensitivity(multiplier: Float) {
        _motionSensitivity.value = multiplier.coerceIn(0.25f, 3f)
    }

    fun setGlassesControlMode(mode: GlassesControlMode) {
        GlassesSessionState.controlMode = mode
        _glassesControlMode.value = mode
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

    fun resetCursor() {
        _cursor.value = CompanionCursorState()
        _motionSensitivity.value = 1f
        _glassesControlMode.value = GlassesSessionState.controlMode
    }
}
