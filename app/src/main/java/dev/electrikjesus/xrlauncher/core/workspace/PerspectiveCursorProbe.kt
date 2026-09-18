package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cursor path used to check Home Space perspective: a 3-turn spiral from the
 * view center out to the display edges (sphere-ray hover, not a flat HUD plane).
 */
object PerspectiveCursorProbe {
    const val ACTION = "dev.electrikjesus.xrlauncher.PERSPECTIVE_PROBE"
    const val DEFAULT_WIDTH_PX = 1920f
    const val DEFAULT_HEIGHT_PX = 1080f
    const val SMALL_RADIUS_PX = 160f

    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    fun requestPlay() {
        _requests.tryEmit(Unit)
    }

    fun spiralSamples(
        turns: Int = 3,
        steps: Int = 216,
        widthPx: Float = DEFAULT_WIDTH_PX,
        heightPx: Float = DEFAULT_HEIGHT_PX,
    ): List<Pair<Float, Float>> {
        val count = steps.coerceAtLeast(turns * 24)
        val maxRadius = hypot(widthPx * 0.5f, heightPx * 0.5f)
        return (0 until count).map { index ->
            val t = if (count == 1) 1f else index / (count - 1).toFloat()
            val angle = t * turns * 2f * PI.toFloat()
            displayPoint(maxRadius * t, angle, widthPx, heightPx)
        }
    }

    fun smallCircleSamples(
        steps: Int,
        widthPx: Float = DEFAULT_WIDTH_PX,
        heightPx: Float = DEFAULT_HEIGHT_PX,
    ): List<Pair<Float, Float>> = circleSamples(SMALL_RADIUS_PX, steps, widthPx, heightPx)

    fun cornerCircleSamples(
        steps: Int,
        widthPx: Float = DEFAULT_WIDTH_PX,
        heightPx: Float = DEFAULT_HEIGHT_PX,
    ): List<Pair<Float, Float>> = circleSamples(
        radiusPx = hypot(widthPx * 0.5f, heightPx * 0.5f),
        steps = steps,
        widthPx = widthPx,
        heightPx = heightPx,
    )

    fun cornerAngles(): FloatArray = floatArrayOf(
        cornerAngle(xSign = -1f, ySign = -1f),
        cornerAngle(xSign = 1f, ySign = -1f),
        cornerAngle(xSign = 1f, ySign = 1f),
        cornerAngle(xSign = -1f, ySign = 1f),
    )

    suspend fun play(
        widthPx: Float = DEFAULT_WIDTH_PX,
        heightPx: Float = DEFAULT_HEIGHT_PX,
    ) {
        _playing.value = true
        try {
            GlassesHomeLook.lookHome()
            for (point in spiralSamples(turns = 3, steps = 216, widthPx = widthPx, heightPx = heightPx)) {
                CompanionPointerBus.setCursorPosition(point.first, point.second)
                delay(22L)
            }
            CompanionPointerBus.recenterCursor()
        } finally {
            _playing.value = false
        }
    }

    private fun circleSamples(
        radiusPx: Float,
        steps: Int,
        widthPx: Float,
        heightPx: Float,
    ): List<Pair<Float, Float>> {
        val count = steps.coerceAtLeast(8)
        return (0 until count).map { index ->
            val angle = (index.toFloat() / count) * 2f * PI.toFloat()
            displayPoint(radiusPx, angle, widthPx, heightPx)
        }
    }

    private fun displayPoint(
        radiusPx: Float,
        angleRad: Float,
        widthPx: Float,
        heightPx: Float,
    ): Pair<Float, Float> {
        val x = ((widthPx * 0.5f) + radiusPx * cos(angleRad)) / widthPx
        val y = ((heightPx * 0.5f) + radiusPx * sin(angleRad)) / heightPx
        return x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
    }

    private fun cornerAngle(xSign: Float, ySign: Float): Float {
        val dx = xSign * DEFAULT_WIDTH_PX * 0.5f
        val dy = ySign * DEFAULT_HEIGHT_PX * 0.5f
        return kotlin.math.atan2(dy, dx)
    }

    private fun angularDistance(a: Float, b: Float): Float {
        var delta = (a - b) % (2f * PI.toFloat())
        if (delta > PI) delta -= 2f * PI.toFloat()
        if (delta < -PI) delta += 2f * PI.toFloat()
        return kotlin.math.abs(delta)
    }
}
