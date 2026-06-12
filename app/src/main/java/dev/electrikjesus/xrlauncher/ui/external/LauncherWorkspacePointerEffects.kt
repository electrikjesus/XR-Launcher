package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

/** Hit-tests companion pointer clicks against launcher item bounds (glasses foreground). */
@Composable
fun LauncherWorkspacePointerEffects(
    apps: List<LaunchableApp>,
    itemBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
) {
    var lastLaunchAtMs by remember { mutableLongStateOf(0L) }

    fun cursorPoint(normalizedX: Float, normalizedY: Float): Offset =
        Offset(normalizedX * rootWidthPx, normalizedY * rootHeightPx)

    fun findAppAt(point: Offset): LaunchableApp? {
        val key = itemBounds.entries.firstOrNull { (_, rect) -> rect.contains(point) }?.key
        return key?.let { k -> apps.find { it.componentName.flattenToString() == k } }
    }

    LaunchedEffect(Unit) {
        CompanionPointerBus.clicks.collect { click ->
            val now = System.currentTimeMillis()
            if (now - lastLaunchAtMs < LAUNCH_DEBOUNCE_MS) return@collect
            val app = findAppAt(cursorPoint(click.x, click.y))
            Log.d(LOG_TAG, "click ${click.button} at (${click.x}, ${click.y}) hit=${app?.label}")
            when (click.button) {
                PointerButton.LEFT -> {
                    if (app != null) {
                        lastLaunchAtMs = now
                        onLaunchApp(app)
                    }
                }
                PointerButton.RIGHT -> {
                    if (app != null) {
                        onToggleHotseatPin(app)
                    }
                }
            }
        }
    }

    val cursor by CompanionPointerBus.cursor.collectAsState()
    LaunchedEffect(cursor.x, cursor.y, itemBounds.size, rootWidthPx, rootHeightPx) {
        CompanionPointerBus.setHoveredLabel(
            findAppAt(cursorPoint(cursor.x, cursor.y))?.label,
        )
    }
}

private const val LOG_TAG = "XRLauncher/Pointer"
private const val LAUNCH_DEBOUNCE_MS = 1_000L
