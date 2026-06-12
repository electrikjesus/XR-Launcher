package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@Composable
fun ExternalDisplayWorkspaceScreen(
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onRightClick: (LaunchableApp?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
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
                PointerButton.RIGHT -> onRightClick(app)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            rootWidthPx = with(density) { maxWidth.toPx() }
            rootHeightPx = with(density) { maxHeight.toPx() }

            LaunchedEffect(cursor.x, cursor.y, itemBounds.size, rootWidthPx, rootHeightPx) {
                CompanionPointerBus.setHoveredLabel(
                    findAppAt(cursorPoint(cursor.x, cursor.y))?.label,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            ) {
                Text(
                    text = stringResource(R.string.glasses_workspace),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.control_mode_launcher_hint),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = Color.White.copy(alpha = 0.7f),
                )
                if (cursor.hoveredLabel != null) {
                    Text(
                        text = stringResource(R.string.cursor_over, cursor.hoveredLabel!!),
                        color = Color(0xFF03DAC5),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                apps.forEach { app ->
                    val key = app.componentName.flattenToString()
                    val isHovered = cursor.hoveredLabel == app.label
                    Text(
                        text = app.label,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .onGloballyPositioned { coordinates ->
                                itemBounds[key] = coordinates.boundsInRoot()
                            }
                            .background(
                                if (isHovered) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isHovered) Color(0xFF03DAC5) else Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            ExternalCursorDot(modifier = Modifier.fillMaxSize())
        }
    }
}

private const val LOG_TAG = "XRLauncher/Pointer"
private const val LAUNCH_DEBOUNCE_MS = 1_000L
