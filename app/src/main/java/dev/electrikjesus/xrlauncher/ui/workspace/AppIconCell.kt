package dev.electrikjesus.xrlauncher.ui.workspace

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import dev.electrikjesus.xrlauncher.core.launcher.AppIconCache
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconCell(
    app: LaunchableApp,
    isHovered: Boolean,
    onBoundsChanged: (String, androidx.compose.ui.geometry.Rect) -> Unit,
    isPinned: Boolean = false,
    onLaunchApp: ((LaunchableApp) -> Unit)? = null,
    onContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    iconSize: Dp = 52.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon: Drawable = remember(app.packageName) {
        AppIconCache.getIcon(context, app.packageName)
    }
    val key = app.componentName.flattenToString()
    var boundsInRoot by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = modifier
            .then(
                when {
                    onLaunchApp != null && onContextMenu != null -> Modifier.combinedClickable(
                        onClick = { onLaunchApp(app) },
                        onLongClick = {
                            boundsInRoot?.let { rect -> onContextMenu(app, rect) }
                        },
                    )
                    onLaunchApp != null -> Modifier.combinedClickable(
                        onClick = { onLaunchApp(app) },
                    )
                    else -> Modifier
                },
            )
            .onGloballyPositioned { coordinates ->
                val rect = coordinates.boundsInRoot()
                boundsInRoot = rect
                onBoundsChanged(key, rect)
            }
            .background(
                when {
                    isHovered -> Color.White.copy(alpha = 0.12f)
                    isPinned -> Color(0xFF03DAC5).copy(alpha = 0.12f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(12.dp),
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    // Normalized square bitmaps from AppIconCache — fill the cell uniformly.
                    scaleType = ImageView.ScaleType.FIT_XY
                    setImageDrawable(icon)
                }
            },
            update = { it.setImageDrawable(icon) },
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = app.label,
            style = if (iconSize >= 72.dp) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.labelSmall
            },
            color = if (isHovered) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.9f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
