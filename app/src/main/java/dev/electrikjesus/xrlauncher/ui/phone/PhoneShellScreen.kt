package dev.electrikjesus.xrlauncher.ui.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.capability.DeviceCapabilities
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneShellScreen(
    apps: List<LaunchableApp>,
    capabilities: DeviceCapabilities,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenGlassesWorkspace: () -> Unit,
    onOpenCompanion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (capabilities.hasSecondaryDisplay) {
                Text(stringResource(R.string.external_display_detected))
                Button(onClick = onOpenGlassesWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Text(stringResource(R.string.open_on_glasses), modifier = Modifier.padding(start = 8.dp))
                }
                Button(onClick = onOpenCompanion, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null)
                    Text(stringResource(R.string.use_as_controller), modifier = Modifier.padding(start = 8.dp))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
            )

            if (filtered.isEmpty()) {
                Text(stringResource(R.string.no_apps_found))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.componentName.flattenToString() }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            supportingContent = { Text(app.packageName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunchApp(app) },
                        )
                    }
                }
            }
        }
    }
}
