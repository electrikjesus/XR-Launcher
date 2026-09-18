package dev.electrikjesus.xrlauncher.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.capability.DeviceCapabilities
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.launcher.PhoneHomeLayout
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingLogic
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingStore
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import dev.electrikjesus.xrlauncher.ui.workspace.AppIconCell
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWallpaper
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

private val PhoneHomeDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xCC1C1B1F),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun PhoneShellScreen(
    apps: List<LaunchableApp>,
    capabilities: DeviceCapabilities,
    hotseatApps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenGlassesWorkspace: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    XRLauncherTheme(forCompanion = true) {
        MaterialTheme(colorScheme = PhoneHomeDarkColors) {
            PhoneShellContent(
                apps = apps,
                capabilities = capabilities,
                hotseatApps = hotseatApps,
                onLaunchApp = onLaunchApp,
                onOpenGlassesWorkspace = onOpenGlassesWorkspace,
                onOpenCompanion = onOpenCompanion,
                onOpenSettings = onOpenSettings,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PhoneShellContent(
    apps: List<LaunchableApp>,
    capabilities: DeviceCapabilities,
    hotseatApps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenGlassesWorkspace: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }
    var showOnboarding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeTick, capabilities.hasSecondaryDisplay) {
        OnboardingStore.init(context)
        val replay = OnboardingStore.consumeReplay(context)
        showOnboarding = OnboardingLogic.shouldShow(
            completed = OnboardingStore.isCompleted(),
            replayRequested = replay,
            hasSecondaryDisplay = capabilities.hasSecondaryDisplay,
        )
    }

    val gridApps = remember(apps, hotseatApps, query) {
        PhoneHomeLayout.gridApps(apps, hotseatApps, query)
    }

    Box(modifier = modifier.fillMaxSize()) {
        WorkspaceWallpaper(
            wallpaperChoice = WorkspaceWallpaperChoice.SYSTEM,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )

        if (showOnboarding) {
            OnboardingScreen(
                onFinished = {
                    OnboardingStore.markCompleted(context)
                    showOnboarding = false
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                PhoneHomeTopBar(
                    onOpenSettings = onOpenSettings,
                    onShowOnboarding = { showOnboarding = true },
                )
                PhoneHomeClock(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.28f),
                        focusedBorderColor = Color.White.copy(alpha = 0.35f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                    ),
                )
                if (gridApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_apps_found),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 80.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        items(gridApps, key = { it.componentName.flattenToString() }) { app ->
                            AppIconCell(
                                app = app,
                                isHovered = false,
                                onBoundsChanged = { _, _ -> },
                                onLaunchApp = onLaunchApp,
                            )
                        }
                    }
                }
                PhoneHomeDock(
                    hotseatApps = hotseatApps,
                    hasSecondaryDisplay = capabilities.hasSecondaryDisplay,
                    onLaunchApp = onLaunchApp,
                    onOpenGlassesWorkspace = onOpenGlassesWorkspace,
                    onOpenCompanion = onOpenCompanion,
                    onShowOnboarding = { showOnboarding = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }
    }
}

@Composable
private fun PhoneHomeTopBar(
    onOpenSettings: () -> Unit,
    onShowOnboarding: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShowOnboarding) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = stringResource(R.string.onboarding_show_again),
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_open),
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun PhoneHomeClock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date(), key1 = Unit) {
        while (true) {
            value = Date()
            delay(30_000)
        }
    }
    Column(modifier = modifier) {
        Text(
            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(now),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
        )
        Text(
            text = DateFormat.getDateInstance(DateFormat.FULL).format(now),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun PhoneHomeDock(
    hotseatApps: List<LaunchableApp>,
    hasSecondaryDisplay: Boolean,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenGlassesWorkspace: () -> Unit,
    onOpenCompanion: () -> Unit,
    onShowOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xE61C1B1F),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hotseatApps.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    hotseatApps.forEach { app ->
                        AppIconCell(
                            app = app,
                            isHovered = false,
                            onBoundsChanged = { _, _ -> },
                            onLaunchApp = onLaunchApp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (hasSecondaryDisplay) {
                Button(
                    onClick = onOpenGlassesWorkspace,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null)
                    Text(
                        text = stringResource(R.string.control_glasses),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                FilledTonalButton(
                    onClick = onOpenCompanion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null)
                    Text(
                        text = stringResource(R.string.use_as_controller),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.phone_connect_glasses_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                OutlinedButton(
                    onClick = onShowOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null)
                    Text(
                        text = stringResource(R.string.connect_glasses),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
