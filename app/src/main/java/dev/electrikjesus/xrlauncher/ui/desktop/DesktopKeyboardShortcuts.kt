package dev.electrikjesus.xrlauncher.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState

@Composable
fun DesktopKeyboardLayer(
    panelCount: Int,
    visiblePanels: List<PanelState>,
    gridApps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onApplyPreset: (LayoutPreset) -> Unit,
    onFocusedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Tab -> {
                        if (event.isShiftPressed) {
                            CompanionPointerBus.focusPreviousPanelIndex(panelCount)
                        } else {
                            CompanionPointerBus.focusNextPanelIndex(panelCount)
                        }
                        onFocusedIndexChanged(CompanionPointerBus.focusedPanelIndex.value)
                        true
                    }
                    Key.DirectionRight -> {
                        CompanionPointerBus.focusNextPanelIndex(panelCount)
                        onFocusedIndexChanged(CompanionPointerBus.focusedPanelIndex.value)
                        true
                    }
                    Key.DirectionLeft -> {
                        CompanionPointerBus.focusPreviousPanelIndex(panelCount)
                        onFocusedIndexChanged(CompanionPointerBus.focusedPanelIndex.value)
                        true
                    }
                    Key.Enter -> {
                        launchFocusedPanelApp(
                            visiblePanels = visiblePanels,
                            focusedIndex = CompanionPointerBus.focusedPanelIndex.value,
                            gridApps = gridApps,
                            hotseatApps = hotseatApps,
                            onLaunchApp = onLaunchApp,
                        )
                        true
                    }
                    Key.One -> {
                        onApplyPreset(LayoutPreset.SINGLE)
                        true
                    }
                    Key.Two -> {
                        onApplyPreset(LayoutPreset.DUAL)
                        true
                    }
                    Key.Three -> {
                        onApplyPreset(LayoutPreset.TRIPTYCH)
                        true
                    }
                    Key.Slash -> {
                        showHelp = !showHelp
                        true
                    }
                    Key.Escape -> {
                        if (showHelp) {
                            showHelp = false
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
    ) {
        content()
        if (showHelp) {
            DesktopShortcutHelpOverlay(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showHelp = false },
            )
        }
    }
}

private fun launchFocusedPanelApp(
    visiblePanels: List<PanelState>,
    focusedIndex: Int,
    gridApps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
) {
    val panel = visiblePanels.getOrNull(focusedIndex) ?: return
    when (panel.kind) {
        PanelKind.APP_DRAWER -> gridApps.firstOrNull()?.let(onLaunchApp)
        PanelKind.HOTSEAT -> hotseatApps.firstOrNull()?.let(onLaunchApp)
        else -> Unit
    }
}

@Composable
private fun DesktopShortcutHelpOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.desktop_shortcuts_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.desktop_shortcuts_body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = stringResource(R.string.desktop_shortcuts_dismiss),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}
