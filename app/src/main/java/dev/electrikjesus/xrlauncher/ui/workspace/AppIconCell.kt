package dev.electrikjesus.xrlauncher.ui.workspace

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import dev.electrikjesus.xrlauncher.core.launcher.AppIconCache
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@Composable
fun AppIconCell(
    app: LaunchableApp,
    isHovered: Boolean,
    onBoundsChanged: (String, androidx.compose.ui.geometry.Rect) -> Unit,
    isPinned: Boolean = false,
    onLaunchApp: ((LaunchableApp) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon: Drawable = remember(app.packageName) {
        AppIconCache.getIcon(context, app.packageName)
    }
    val key = app.componentName.flattenToString()

    Column(
        modifier = modifier
            .then(
                if (onLaunchApp != null) {
                    Modifier.clickable { onLaunchApp(app) }
                } else {
                    Modifier
                },
            )
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(key, coordinates.boundsInRoot())
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
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(icon)
                }
            },
            update = { it.setImageDrawable(icon) },
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHovered) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.9f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
