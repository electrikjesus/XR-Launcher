package dev.electrikjesus.xrlauncher

import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.electrikjesus.xrlauncher.core.capability.CapabilityDetector
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingStore
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingController
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
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
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        GlassesSessionState.appContext = applicationContext
        OnboardingStore.init(this)
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
        handleUsbIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshCapabilities()
    }

    override fun onDestroy() {
        getSystemService(DisplayManager::class.java)
            .unregisterDisplayListener(displayListener)
        GlassesSessionState.panelEmbedRegistry?.disposeAll()
        GlassesSessionState.panelEmbedRegistry = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshCapabilities()
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent?) {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
        GlassesSessionState.rayNeoUsbAttached = RayNeoHeadTrackingController.isRayNeoAttached(this)
        if (GlassesSessionState.xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING) {
            RayNeoHeadTrackingController.start(this)
        }
        // Expanded host already running: offer what to put on the glasses display.
        if (GlassesSessionState.hostImmersiveSession) {
            refreshCapabilities()
            if (
                !GlassesSessionState.externalWorkspaceActive &&
                capabilityDetector.capabilities.value.hasSecondaryDisplay
            ) {
                HomeSpaceDialogState.openGlassesDisplay()
            }
        }
    }

    private fun refreshCapabilities() {
        capabilityDetector.refresh()
    }
}
