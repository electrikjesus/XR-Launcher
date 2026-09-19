package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.settings.SettingsScreen
import dev.electrikjesus.xrlauncher.ui.workspace.PanelTextureCapture

private val CardBg = Color(0xF21C1C1E)

/** In-engine Settings dialog for immersive host / glasses Home Space. */
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
    PanelTextureCapture(
        panelId = HomeSpaceDialogState.SETTINGS_TEXTURE_ID,
        centerXNorm = 0.5f,
        centerYNorm = 0.5f,
        enabled = true,
        drawToScreen = false,
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(4f)
            .widthIn(min = 560.dp, max = 820.dp)
            .heightIn(max = 900.dp)
            .fillMaxWidth(0.55f)
            .fillMaxHeight(0.82f),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(CardBg)
                .padding(8.dp),
        ) {
            SettingsScreen(
                workspaceRepository = workspaceRepository,
                onNavigateBack = onClose,
                onShowOnboarding = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
