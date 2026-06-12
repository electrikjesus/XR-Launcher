package dev.electrikjesus.xrlauncher.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@Composable
fun AppGridPanel(
    title: String,
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onFocus)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        apps.forEach { app ->
            Text(
                text = app.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLaunchApp(app) }
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
