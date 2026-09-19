package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.HostInputMethod
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceScaledLayer
import kotlinx.coroutines.launch

private val HudBg = Color(0xCC1C1C1E)
private val Accent = Color(0xFF8AB4F8)

/** Screen-locked top HUD mirroring companion touchpad actions (2.27–2.28). */
@Composable
fun BoxScope.HostXrChromeBar(
    appearance: WorkspaceAppearance,
    hoveredLabel: String?,
    workspaceRepository: WorkspaceRepository?,
    onBoundsChanged: (String, Rect) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val lookMode by GlassesLookMode.preferenceFlow.collectAsState(initial = GlassesLookMode.preference)
    val xrInputMode by GlassesSessionState.xrInputModeFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val tuned = appearance.clamped()

    WorkspaceScaledLayer(uiScale = tuned.uiScale) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f)
                .padding(top = 20.dp)
                .clip(CircleShape)
                .background(HudBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HudIcon(
                icon = Icons.Default.TouchApp,
                boundsKey = GlassesHomeHits.HUD_INPUT_TOUCHPAD,
                hovered = hoveredLabel == GlassesHomeHits.HUD_INPUT_TOUCHPAD_LABEL,
                selected = xrInputMode == GlassesXrInputMode.COMPANION,
                contentDescription = stringResource(R.string.companion_cursor_touchpad),
                onBoundsChanged = onBoundsChanged,
                onClick = {
                    GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
                },
            )
            HudIcon(
                icon = Icons.Default.Visibility,
                boundsKey = GlassesHomeHits.HUD_INPUT_HEAD,
                hovered = hoveredLabel == GlassesHomeHits.HUD_INPUT_HEAD_LABEL,
                selected = xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING,
                contentDescription = stringResource(R.string.companion_cursor_head),
                onBoundsChanged = onBoundsChanged,
                onClick = {
                    if (GlassesSessionState.rayNeoUsbAttached) {
                        GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
                        CompanionPointerBus.recenterCursor()
                    }
                },
            )
            HudIcon(
                icon = Icons.Default.Mouse,
                boundsKey = GlassesHomeHits.HUD_LOOK_MODE,
                hovered = hoveredLabel == GlassesHomeHits.HUD_LOOK_MODE_LABEL,
                selected = lookMode == GlassesLookMode.FPS,
                contentDescription = stringResource(R.string.companion_mouselook),
                onBoundsChanged = onBoundsChanged,
                onClick = {
                    val next = if (lookMode == GlassesLookMode.FPS) {
                        GlassesLookMode.GRADIENT
                    } else {
                        GlassesLookMode.FPS
                    }
                    GlassesLookMode.preference = next
                    // Absolute host keeps the on-screen cursor; only companion FPS re-locks center.
                    if (next == GlassesLookMode.FPS && !HostInputMethod.usesAbsoluteHostCursor()) {
                        CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
                    }
                    val repo = workspaceRepository ?: return@HudIcon
                    scope.launch {
                        repo.updateAppearance(tuned.copy(lookMode = next))
                    }
                },
            )
            HudIcon(
                icon = Icons.Default.FilterCenterFocus,
                boundsKey = GlassesHomeHits.HUD_RECENTER,
                hovered = hoveredLabel == GlassesHomeHits.HUD_RECENTER_LABEL,
                selected = false,
                contentDescription = stringResource(R.string.recenter),
                onBoundsChanged = onBoundsChanged,
                onClick = {
                    CompanionPointerBus.recenterCursor()
                    GlassesHomeLook.lookHome()
                },
            )
            HudIcon(
                icon = Icons.Default.Keyboard,
                boundsKey = GlassesHomeHits.HUD_KEYBOARD,
                hovered = hoveredLabel == GlassesHomeHits.HUD_KEYBOARD_LABEL,
                selected = false,
                contentDescription = stringResource(R.string.companion_show_keyboard),
                onBoundsChanged = onBoundsChanged,
                onClick = {
                    CompanionPointerBus.setTextEntryActive(true)
                    keyboardController?.show()
                },
            )
            HudIcon(
                icon = Icons.Default.Settings,
                boundsKey = GlassesHomeHits.HUD_SETTINGS,
                hovered = hoveredLabel == GlassesHomeHits.HUD_SETTINGS_LABEL,
                selected = false,
                contentDescription = stringResource(R.string.settings_title),
                onBoundsChanged = onBoundsChanged,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun HudIcon(
    icon: ImageVector,
    boundsKey: String,
    hovered: Boolean,
    selected: Boolean,
    contentDescription: String,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                when {
                    hovered -> Accent.copy(alpha = 0.55f)
                    selected -> Accent.copy(alpha = 0.28f)
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}
