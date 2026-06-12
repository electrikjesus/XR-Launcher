package dev.electrikjesus.xrlauncher

import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.electrikjesus.xrlauncher.core.capability.CapabilityDetector
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme

class MainActivity : ComponentActivity() {
    private lateinit var capabilityDetector: CapabilityDetector

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshCapabilities()
        override fun onDisplayRemoved(displayId: Int) = refreshCapabilities()
        override fun onDisplayChanged(displayId: Int) = refreshCapabilities()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        capabilityDetector = CapabilityDetector(this)
        getSystemService(DisplayManager::class.java)
            .registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

        setContent {
            XRLauncherTheme {
                XRLauncherApp(
                    capabilityDetector = capabilityDetector,
                    onRefreshCapabilities = ::refreshCapabilities,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCapabilities()
    }

    override fun onDestroy() {
        getSystemService(DisplayManager::class.java)
            .unregisterDisplayListener(displayListener)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        refreshCapabilities()
    }

    private fun refreshCapabilities() {
        capabilityDetector.refresh()
    }
}
