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
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.settings.SettingsScreen
import dev.electrikjesus.xrlauncher.ui.workspace.PanelTextureCapture

private val CardBg = Color(0xF21C1C1E)
private val ScrimBg = Color(0x99000000)

/**
 * Host Settings as a screen-space modal (Minecraft inventory style): Compose draws and
 * receives clicks; GLES billboard is skipped while [dev.electrikjesus.xrlauncher.core.display.GlassesSessionState.hostImmersiveSession].
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
    ) {
        PanelTextureCapture(
            panelId = HomeSpaceDialogState.SETTINGS_TEXTURE_ID,
            centerXNorm = 0.5f,
            centerYNorm = 0.5f,
            enabled = true,
            drawToScreen = true,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 560.dp, max = 820.dp)
                .heightIn(max = 900.dp)
                .fillMaxWidth(0.55f)
                .fillMaxHeight(0.82f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
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
}
