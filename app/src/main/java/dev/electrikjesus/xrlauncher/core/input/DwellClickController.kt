package dev.electrikjesus.xrlauncher.core.input

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Runtime dwell dial: fed by pointer hover ticks; fires FPS-center left clicks. */
object DwellClickController {
    private val progressHelper = DwellClickProgress()
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var targetKey: String? = null
    private var pressed: Boolean = false
    private var lastTickMs: Long = 0L

    fun onTarget(key: String?, isPressed: Boolean) {
        targetKey = key
        pressed = isPressed
        if (!DwellClickStore.isActive() || key == null || isPressed) {
            progressHelper.reset()
            _progress.value = 0f
            lastTickMs = 0L
        }
    }

    /**
     * Advance the dial. Call from a ~60 Hz loop while the glasses workspace is showing.
     * @return true if a click was fired this frame.
     */
    fun tick(): Boolean {
        if (!DwellClickStore.isActive()) {
            if (_progress.value != 0f) {
                progressHelper.reset()
                _progress.value = 0f
            }
            lastTickMs = 0L
            return false
        }
        progressHelper.dwellMs = DwellClickStore.dwellMs.value
        val now = SystemClock.uptimeMillis()
        val deltaMs = if (lastTickMs == 0L) 0f else (now - lastTickMs).toFloat().coerceIn(0f, 50f)
        lastTickMs = now
        val fired = progressHelper.tick(
            active = true,
            targetKey = targetKey,
            pressed = pressed,
            deltaMs = deltaMs,
        )
        _progress.value = progressHelper.progress
        if (fired) {
            CompanionPointerBus.clickAt(0.5f, 0.5f, PointerButton.LEFT)
            return true
        }
        return false
    }
}
