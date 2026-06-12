package dev.electrikjesus.xrlauncher.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.ui.shared.AppGridPanel

@Composable
fun SpatialDesktopScreen(
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val camera by CompanionPointerBus.camera.collectAsState()
    val focusedPanel by CompanionPointerBus.focusedPanelIndex.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    CompanionPointerBus.orbitCamera(
                        deltaYaw = dragAmount.x * 0.08f,
                        deltaPitch = -dragAmount.y * 0.08f,
                    )
                }
            }
            .graphicsLayer {
                rotationY = camera.yawDegrees
                rotationX = camera.pitchDegrees
            },
    ) {
        Subspace {
            SpatialCurvedRow(curveRadius = 825.dp) {
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(420.dp)
                        .height(640.dp)
                        .movable()
                        .resizable(),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (focusedPanel == 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        AppGridPanel(
                            title = stringResource(R.string.workspace_panel_drawer),
                            apps = apps.take(12),
                            onLaunchApp = onLaunchApp,
                            onFocus = { CompanionPointerBus.setFocusedPanel(0) },
                        )
                    }
                }
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(360.dp)
                        .height(480.dp)
                        .movable()
                        .resizable(),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (focusedPanel == 1) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.workspace_panel_status),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.spatial_workspace),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                text = "Yaw ${camera.yawDegrees.toInt()}° · Pitch ${camera.pitchDegrees.toInt()}°",
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
