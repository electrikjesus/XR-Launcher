package dev.electrikjesus.xrlauncher.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.electrikjesus.xrlauncher.ui.companion.CompanionTouchpadScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme

class CompanionControllerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XRLauncherTheme {
                CompanionTouchpadScreen()
            }
        }
    }
}
