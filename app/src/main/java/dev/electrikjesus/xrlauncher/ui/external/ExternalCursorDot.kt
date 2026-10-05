package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.CursorStyles
import dev.electrikjesus.xrlauncher.core.input.DwellClickController
import dev.electrikjesus.xrlauncher.core.input.DwellClickStore
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import kotlin.math.roundToInt

/** In-activity cursor for Launcher mode on the glasses display. */
@Composable
fun ExternalCursorDot(
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val dwellProgress by DwellClickController.progress.collectAsState()
    val dwellWanted by DwellClickStore.enabledWanted.collectAsState()
    val lookMode by GlassesLookMode.preferenceFlow.collectAsState()
    val xrInputMode by GlassesSessionState.xrInputModeFlow.collectAsState()
    val launcherForeground by GlassesSessionState.launcherForegroundFlow.collectAsState()
    val style = CursorStyles.launcher
    val dwellAllowed = lookMode == GlassesLookMode.FPS &&
        xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING &&
        (launcherForeground || GlassesSessionState.hostImmersiveSession)
    val showDial = dwellWanted && dwellAllowed && dwellProgress > 0f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }
        val cursorXPx = cursor.x * rootWidthPx
        val cursorYPx = cursor.y * rootHeightPx
        val halfPx = with(density) { style.halfDotSize.toPx() }
        val dialSize = style.dotSize * 2.4f

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cursorXPx - halfPx).roundToInt(),
                        (cursorYPx - halfPx).roundToInt(),
                    )
                }
                .size(style.dotSize)
                .clip(CircleShape)
                .alpha(style.dotAlpha)
                .background(
                    if (cursor.isPressed) Color(0xFFBB86FC) else Color(0xFF03DAC5),
                ),
        )
        if (showDial) {
            val dialHalfPx = with(density) { dialSize.toPx() / 2f }
            Canvas(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (cursorXPx - dialHalfPx).roundToInt(),
                            (cursorYPx - dialHalfPx).roundToInt(),
                        )
                    }
                    .size(dialSize),
            ) {
                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                val pad = stroke.width
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2f, size.height - pad * 2f),
                    style = stroke,
                )
                drawArc(
                    color = Color(0xFF03DAC5),
                    startAngle = -90f,
                    sweepAngle = 360f * dwellProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2f, size.height - pad * 2f),
                    style = stroke,
                )
            }
        }
    }
}
