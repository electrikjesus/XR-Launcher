package dev.electrikjesus.xrlauncher.core.workspace

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Previous / next pane navigation for the large-screen arrows: Desktop wall · Home ·
 * app planes · Tray. Eases [GlassesHomeLook.panNorm] so the target pane ends up centered.
 */
object HomeSpacePaneNav {
    const val ANIMATION_MS = 420L
    private const val FRAME_MS = 16L

    /** Already-centered panes are skipped so one click always moves to a neighbor. */
    private const val CENTER_EPSILON = 0.15f

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    private var job: Job? = null

    fun stops(appPlanes: List<GlassesAppPlane> = GlassesHomeLook.appPlanes): List<Float> =
        listOf(GlassesHomeLook.PANE_LEFT) +
            GlassesHomeLook.homeSpaceSlots(appPlanes).map { it.worldX }

    /**
     * Pane position to center after one step in [direction] (-1 left, +1 right), or null at
     * the ends. [cyclePanes] is one full turn in pane units so full-circle look still resolves
     * the nearest copy of each stop.
     */
    fun neighborTarget(
        current: Float,
        stops: List<Float>,
        direction: Int,
        cyclePanes: Float,
    ): Float? {
        if (stops.isEmpty() || direction == 0) return null
        val mid = (stops.first() + stops.last()) * 0.5f
        val offset = if (cyclePanes > 0f) round((current - mid) / cyclePanes) * cyclePanes else 0f
        val local = current - offset
        val next = if (direction > 0) {
            stops.firstOrNull { it > local + CENTER_EPSILON }
        } else {
            stops.lastOrNull { it < local - CENTER_EPSILON }
        }
        return next?.plus(offset)
    }

    fun target(direction: Int): Float? = neighborTarget(
        current = GlassesHomeLook.panNorm,
        stops = stops(),
        direction = direction,
        cyclePanes = 360f / GlassesHomeLook.lastPaneArcDegrees,
    )

    fun step(direction: Int): Boolean {
        val target = target(direction) ?: return false
        animateTo(target)
        return true
    }

    private fun animateTo(targetPan: Float) {
        job?.cancel()
        val startPan = GlassesHomeLook.panNorm
        val startPitch = GlassesHomeLook.lookPitch
        job = scope.launch {
            val startMs = SystemClock.uptimeMillis()
            while (true) {
                val t = ((SystemClock.uptimeMillis() - startMs) / ANIMATION_MS.toFloat()).coerceIn(0f, 1f)
                val eased = easeInOutCubic(t)
                GlassesHomeLook.panNorm = startPan + (targetPan - startPan) * eased
                GlassesHomeLook.lookPitch = startPitch * (1f - eased)
                if (t >= 1f) break
                delay(FRAME_MS)
            }
        }
    }

    internal fun easeInOutCubic(t: Float): Float =
        if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
}
