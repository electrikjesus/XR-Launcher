package dev.electrikjesus.xrlauncher.core.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import kotlin.math.hypot

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
    /** Normalized distance above which pointer-up becomes drag instead of click. */
    private const val DRAG_THRESHOLD = 0.012f

    private var gestureAnchorX: Float? = null
    private var gestureAnchorY: Float? = null
    private var gesturePressCount = 0
    private var leftButtonInGesture = false
    private var touchpadInGesture = false

    private val _events = MutableSharedFlow<PointerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PointerEvent> = _events.asSharedFlow()

    private val _clicks = MutableSharedFlow<PointerClick>(extraBufferCapacity = 16)
    val clicks: SharedFlow<PointerClick> = _clicks.asSharedFlow()

    private val _camera = MutableStateFlow(WorkspaceCameraState())
    val camera: StateFlow<WorkspaceCameraState> = _camera.asStateFlow()

    private val _focusedPanelIndex = MutableStateFlow(0)
    val focusedPanelIndex: StateFlow<Int> = _focusedPanelIndex.asStateFlow()

    private val _focusedPanelId = MutableStateFlow<String?>(null)
    val focusedPanelId: StateFlow<String?> = _focusedPanelId.asStateFlow()

    private val _cursor = MutableStateFlow(CompanionCursorState())
    val cursor: StateFlow<CompanionCursorState> = _cursor.asStateFlow()

    private val _motionControlEnabled = MutableStateFlow(false)
    val motionControlEnabled: StateFlow<Boolean> = _motionControlEnabled.asStateFlow()

    private val _motionSensitivity = MutableStateFlow(1f)
    val motionSensitivity: StateFlow<Float> = _motionSensitivity.asStateFlow()

    private val _touchpadSensitivity = MutableStateFlow(1f)
    val touchpadSensitivity: StateFlow<Float> = _touchpadSensitivity.asStateFlow()

    private val _glassesControlMode = MutableStateFlow(GlassesSessionState.controlMode)
    val glassesControlMode: StateFlow<GlassesControlMode> = _glassesControlMode.asStateFlow()

    private val _textEntryActive = MutableStateFlow(false)
    val textEntryActiveFlow: StateFlow<Boolean> = _textEntryActive.asStateFlow()

    private var manualPrecisionPointer = false

    /** When true, touchpad double-tap click is disabled — use Left button for keys and menus. */
    fun isTouchpadClickSuppressed(): Boolean = manualPrecisionPointer || _textEntryActive.value

    fun setManualPrecisionPointer(enabled: Boolean) {
        manualPrecisionPointer = enabled
    }

    fun setTextEntryActive(active: Boolean) {
        if (_textEntryActive.value != active) {
            _textEntryActive.value = active
        }
    }

    fun emit(event: PointerEvent) {
        if (event.action == PointerAction.MOVE) {
            val scale = TOUCHPAD_SENSITIVITY * _touchpadSensitivity.value
            moveBy(event.deltaX * scale, event.deltaY * scale)
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
        when (button) {
            PointerButton.LEFT -> {
                deliverLeftClick(current.x, current.y)
                flashPressed()
            }
            PointerButton.RIGHT -> deliverRightClick(current.x, current.y)
        }
    }

    /** Touchpad or left-button press — anchor for click vs drag on final release. */
    fun beginPointerGesture() {
        val current = _cursor.value
        if (gesturePressCount++ == 0) {
            gestureAnchorX = current.x
            gestureAnchorY = current.y
        }
        _cursor.value = current.copy(isPressed = true)
    }

    fun beginLeftButton() {
        leftButtonInGesture = true
        beginPointerGesture()
    }

    fun endLeftButton() = finishPointerGesture(fromTouchpad = false)

    /** Second tap of a double-tap-and-hold — anchors click-drag on the glasses display. */
    fun beginTouchpadDragGesture() {
        touchpadInGesture = true
        beginPointerGesture()
    }

    fun endTouchpadDragGesture() {
        if (!touchpadInGesture) return
        finishPointerGesture(fromTouchpad = true)
    }

    private fun finishPointerGesture(fromTouchpad: Boolean) {
        if (gesturePressCount <= 0) return
        gesturePressCount--
        if (gesturePressCount > 0) {
            _cursor.value = _cursor.value.copy(isPressed = true)
            return
        }
        val startX = gestureAnchorX
        val startY = gestureAnchorY
        gestureAnchorX = null
        gestureAnchorY = null
        val hadLeftButton = leftButtonInGesture
        val hadTouchpadDrag = fromTouchpad && touchpadInGesture
        touchpadInGesture = false
        leftButtonInGesture = false
        val end = _cursor.value.copy(isPressed = false)
        _cursor.value = end
        if (startX == null || startY == null) return
        val moved = hypot(end.x - startX, end.y - startY) > DRAG_THRESHOLD
        val primaryGesture = hadLeftButton || hadTouchpadDrag
        when {
            moved && primaryGesture && shouldInjectPointerOnGlasses() ->
                DisplayPointerInjector.dispatchDrag(
                    GlassesSessionState.secondaryDisplayId!!,
                    startX,
                    startY,
                    end.x,
                    end.y,
                    mapViaLauncherFrame = GlassesSessionState.launcherForeground,
                )
            !moved && primaryGesture -> deliverLeftClick(end.x, end.y)
        }
    }

    private fun pointerInjectionAvailable(): Boolean =
        GlassesSessionState.secondaryDisplayId != null &&
            DisplayPointerInjector.isAvailable

    /** Inject OS gestures when accessibility service is active. */
    private fun shouldInjectPointerOnGlasses(): Boolean = pointerInjectionAvailable()

    private fun deliverLeftClick(x: Float, y: Float) {
        if (shouldInjectPointerOnGlasses()) {
            injectClickAt(x, y, PointerButton.LEFT)
        } else {
            emitClick(PointerClick(button = PointerButton.LEFT, x = x, y = y))
        }
    }

    private fun deliverRightClick(x: Float, y: Float) {
        if (GlassesSessionState.launcherForeground || !pointerInjectionAvailable()) {
            emitClick(PointerClick(button = PointerButton.RIGHT, x = x, y = y))
        } else {
            injectClickAt(x, y, PointerButton.RIGHT)
        }
    }

    private fun injectClickAt(x: Float, y: Float, button: PointerButton) {
        val displayId = GlassesSessionState.secondaryDisplayId ?: return
        DisplayPointerInjector.dispatchClick(displayId, x, y, button, GlassesSessionState.launcherForeground)
    }

    private fun flashPressed() {
        val current = _cursor.value
        _cursor.value = current.copy(isPressed = true)
        _cursor.value = current.copy(isPressed = false)
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

    fun setTouchpadSensitivity(multiplier: Float) {
        _touchpadSensitivity.value = multiplier.coerceIn(0.25f, 3f)
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

    fun focusNextPanelIndex(panelCount: Int) {
        if (panelCount <= 0) return
        _focusedPanelIndex.value = (_focusedPanelIndex.value + 1) % panelCount
    }

    fun focusPreviousPanelIndex(panelCount: Int) {
        if (panelCount <= 0) return
        _focusedPanelIndex.value = (_focusedPanelIndex.value - 1 + panelCount) % panelCount
    }

    fun setFocusedPanelId(panelId: String?) {
        if (_focusedPanelId.value != panelId) {
            _focusedPanelId.value = panelId
        }
    }

    fun focusNextPanel(panelIds: List<String>) {
        if (panelIds.isEmpty()) return
        val current = _focusedPanelId.value
        val currentIndex = panelIds.indexOf(current)
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % panelIds.size
        _focusedPanelId.value = panelIds[nextIndex]
    }

    fun resetCursor() {
        gestureAnchorX = null
        gestureAnchorY = null
        gesturePressCount = 0
        leftButtonInGesture = false
        touchpadInGesture = false
        _cursor.value = CompanionCursorState()
        _motionSensitivity.value = 1f
        _touchpadSensitivity.value = 1f
        _glassesControlMode.value = GlassesSessionState.controlMode
        _textEntryActive.value = false
        manualPrecisionPointer = false
        _focusedPanelId.value = null
        _focusedPanelIndex.value = 0
    }
}
