package dev.electrikjesus.xrlauncher.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfigStore
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.settings.SettingsScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AllAppsGridConfigStore.init(this)

        setContent {
            val workspaceRepository = remember { WorkspaceRepository(applicationContext) }
            XRLauncherTheme {
                SettingsScreen(
                    workspaceRepository = workspaceRepository,
                    onNavigateBack = { finish() },
                )
            }
        }
    }
}
