package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpacePaneNav

private val ArrowBg = Color(0xCC1C1C1E)
private val Accent = Color(0xFF8AB4F8)

/** Screen-locked left/right arrows that glide the view to the previous / next pane. */
@Composable
fun BoxScope.HostPaneNavArrows(
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
) {
    val panNorm by GlassesHomeLook.panNormFlow.collectAsState()
    val appPlanes by GlassesHomeLook.appPlanesFlow.collectAsState()
    val hasPrev = remember(panNorm, appPlanes) { HomeSpacePaneNav.target(-1) != null }
    val hasNext = remember(panNorm, appPlanes) { HomeSpacePaneNav.target(1) != null }
    LaunchedEffect(hasPrev) {
        if (!hasPrev) onBoundsChanged(GlassesHomeHits.HUD_PANE_PREV, Rect.Zero)
    }
    LaunchedEffect(hasNext) {
        if (!hasNext) onBoundsChanged(GlassesHomeHits.HUD_PANE_NEXT, Rect.Zero)
    }
    if (hasPrev) {
        PaneArrow(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            boundsKey = GlassesHomeHits.HUD_PANE_PREV,
            hovered = hoveredLabel == GlassesHomeHits.HUD_PANE_PREV_LABEL,
            contentDescription = stringResource(R.string.host_pane_prev),
            onBoundsChanged = onBoundsChanged,
            onClick = { HomeSpacePaneNav.step(-1) },
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
    if (hasNext) {
        PaneArrow(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            boundsKey = GlassesHomeHits.HUD_PANE_NEXT,
            hovered = hoveredLabel == GlassesHomeHits.HUD_PANE_NEXT_LABEL,
            contentDescription = stringResource(R.string.host_pane_next),
            onBoundsChanged = onBoundsChanged,
            onClick = { HomeSpacePaneNav.step(1) },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun PaneArrow(
    icon: ImageVector,
    boundsKey: String,
    hovered: Boolean,
    contentDescription: String,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .zIndex(8f)
            .padding(horizontal = 20.dp)
            .size(64.dp)
            .clip(CircleShape)
            .background(if (hovered) Accent.copy(alpha = 0.55f) else ArrowBg)
            .clickable(onClick = onClick)
            .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}
