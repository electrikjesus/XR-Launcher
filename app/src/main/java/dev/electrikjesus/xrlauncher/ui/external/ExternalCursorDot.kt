package dev.electrikjesus.xrlauncher.ui.external

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import kotlin.math.roundToInt

@Composable
fun ExternalCursorDot(modifier: Modifier = Modifier) {
    val cursor by CompanionPointerBus.cursor.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }
        val cursorXPx = cursor.x * rootWidthPx
        val cursorYPx = cursor.y * rootHeightPx

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cursorXPx - with(density) { 12.dp.toPx() }).roundToInt(),
                        (cursorYPx - with(density) { 12.dp.toPx() }).roundToInt(),
                    )
                }
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (cursor.isPressed) Color(0xFFBB86FC) else Color(0xFF03DAC5),
                ),
        )
    }
}
