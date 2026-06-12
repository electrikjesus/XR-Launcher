package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R

object PanelChromeBounds {
    fun closeKey(panelId: String): String = "${panelId}__close__"
    fun minimizeKey(panelId: String): String = "${panelId}__minimize__"
    fun restoreKey(panelId: String): String = "${panelId}__restore__"
}

@Composable
fun PanelChromeHeader(
    panelId: String,
    title: String,
    isFocused: Boolean,
    minimized: Boolean,
    showWindowControls: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    onMinimize: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    dragModifier: Modifier = Modifier,
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
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = dragModifier
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    onPanelBoundsChanged(PanelHandleBounds.dragKey(panelId), coordinates.boundsInRoot())
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
        if (showWindowControls) {
            if (minimized) {
                IconButton(
                    onClick = { onRestore?.invoke() },
                    modifier = Modifier
                        .size(40.dp)
                        .onGloballyPositioned { coordinates ->
                            onPanelBoundsChanged(
                                PanelChromeBounds.restoreKey(panelId),
                                coordinates.boundsInRoot(),
                            )
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = stringResource(R.string.panel_restore),
                        tint = focusColor,
                    )
                }
            } else {
                IconButton(
                    onClick = { onMinimize?.invoke() },
                    modifier = Modifier
                        .size(40.dp)
                        .onGloballyPositioned { coordinates ->
                            onPanelBoundsChanged(
                                PanelChromeBounds.minimizeKey(panelId),
                                coordinates.boundsInRoot(),
                            )
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = stringResource(R.string.panel_minimize),
                        tint = if (isFocused) focusColor else Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            IconButton(
                onClick = { onClose?.invoke() },
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned { coordinates ->
                        onPanelBoundsChanged(
                            PanelChromeBounds.closeKey(panelId),
                            coordinates.boundsInRoot(),
                        )
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.panel_close),
                    tint = if (isFocused) focusColor else Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}
