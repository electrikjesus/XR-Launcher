package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

/** Hit-tests right-clicks for hotseat pin; left clicks inject via accessibility + Compose clickables. */
@Composable
fun LauncherWorkspacePointerEffects(
    apps: List<LaunchableApp>,
    itemBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
) {
    fun cursorPoint(normalizedX: Float, normalizedY: Float): Offset =
        Offset(normalizedX * rootWidthPx, normalizedY * rootHeightPx)

    fun findAppAt(point: Offset): LaunchableApp? {
        val key = itemBounds.entries.firstOrNull { (_, rect) -> rect.contains(point) }?.key
        return key?.let { k -> apps.find { it.componentName.flattenToString() == k } }
    }

    LaunchedEffect(Unit) {
        CompanionPointerBus.clicks.collect { click ->
            if (click.button != PointerButton.RIGHT) return@collect
            val app = findAppAt(cursorPoint(click.x, click.y))
            Log.d(
                LOG_TAG,
                "right-click at (${click.x}, ${click.y}) bounds=${itemBounds.size} hit=${app?.label}",
            )
            if (app != null) {
                onToggleHotseatPin(app)
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
