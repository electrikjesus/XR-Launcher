package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object PanelHandleBounds {
    fun dragKey(panelId: String): String = "${panelId}__drag_handle__"
    fun resizeKey(panelId: String): String = "${panelId}__resize_handle__"
}

object PanelHandleMetrics {
    val dragHandleMinHeight = 52.dp
    val resizeHandleSize = 48.dp
}

@Composable
fun PanelDragHandleBar(
    panelId: String,
    title: String,
    isFocused: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusColor = Color(0xFF03DAC5)
    val background = if (isFocused) {
        focusColor.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PanelHandleMetrics.dragHandleMinHeight)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (isFocused) focusColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .onGloballyPositioned { coordinates ->
                onPanelBoundsChanged(PanelHandleBounds.dragKey(panelId), coordinates.boundsInRoot())
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = null,
            tint = if (isFocused) focusColor else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isFocused) focusColor else Color.White.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun PanelResizeHandle(
    panelId: String,
    isFocused: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusColor = Color(0xFF03DAC5)
    val handleColor = if (isFocused) focusColor else Color(0xFF03DAC5).copy(alpha = 0.75f)
    Box(
        modifier = modifier
            .size(PanelHandleMetrics.resizeHandleSize)
            .shadow(if (isFocused) 6.dp else 4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1520).copy(alpha = 0.92f))
            .border(
                width = 2.dp,
                color = handleColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
            )
            .onGloballyPositioned { coordinates ->
                onPanelBoundsChanged(PanelHandleBounds.resizeKey(panelId), coordinates.boundsInRoot())
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(22.dp)
                .border(
                    width = 3.dp,
                    color = handleColor,
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                ),
        )
    }
}
