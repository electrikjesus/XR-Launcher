package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.settings.SettingsScreen

private val CardBg = Color(0xF21C1C1E)
private val ScrimBg = Color(0x99000000)

/**
 * Host Settings as a screen-space modal. The scrim is a sibling behind the card, not a
 * clickable parent: a parent clickable plus [androidx.compose.material3.Slider] leaves the
 * press gesture stuck, so only sliders keep receiving events. No offscreen capture either —
 * [dev.electrikjesus.xrlauncher.ui.workspace.PanelTextureCapture] records into a graphics
 * layer and breaks hit testing after the first drag.
 */
@Composable
fun BoxScope.HostSettingsDialogLayer(
    workspaceRepository: WorkspaceRepository,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onClose: () -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedHover = hoveredLabel
    @Suppress("UNUSED_PARAMETER")
    val unusedBounds = onBoundsChanged
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(4f)
            .background(ScrimBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    )
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(5f)
            .widthIn(min = 360.dp, max = 520.dp)
            .heightIn(max = 640.dp)
            .fillMaxWidth(0.46f)
            .fillMaxHeight(0.72f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(8.dp),
    ) {
        SettingsScreen(
            workspaceRepository = workspaceRepository,
            onNavigateBack = onClose,
            onShowOnboarding = onClose,
            homeSpaceOnly = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
