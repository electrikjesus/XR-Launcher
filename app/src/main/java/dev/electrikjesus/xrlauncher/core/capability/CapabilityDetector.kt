package dev.electrikjesus.xrlauncher.core.capability

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CapabilityDetector(context: Context) {
    private val appContext = context.applicationContext
    private val displayManager =
        appContext.getSystemService(DisplayManager::class.java)

    private val _capabilities = MutableStateFlow(readCapabilities())
    val capabilities: StateFlow<DeviceCapabilities> = _capabilities.asStateFlow()

    fun refresh() {
        _capabilities.value = readCapabilities()
    }

    fun readCapabilities(): DeviceCapabilities {
        val metrics = appContext.resources.displayMetrics
        val smallestWidthDp = (metrics.widthPixels.coerceAtMost(metrics.heightPixels) /
            metrics.density).toInt()
        val formFactor = CapabilityLogic.resolveFormFactor(smallestWidthDp)
        val secondaryDisplayIds = findSecondaryDisplayIds()
        val hasSecondaryDisplay = secondaryDisplayIds.isNotEmpty()
        val hasSpatialApi = appContext.packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)
        val tier = CapabilityLogic.resolveTier(
            formFactor = formFactor,
            hasSecondaryDisplay = hasSecondaryDisplay,
            hasSpatialApi = hasSpatialApi,
            isProjectedGlassesConnected = hasSecondaryDisplay && formFactor == LayoutFormFactor.COMPACT,
        )
        return DeviceCapabilities(
            tier = tier,
            formFactor = formFactor,
            hasSecondaryDisplay = hasSecondaryDisplay,
            hasSpatialApi = hasSpatialApi,
            secondaryDisplayIds = secondaryDisplayIds,
        )
    }

    private fun findSecondaryDisplayIds(): List<Int> {
        val defaultDisplayId = Display.DEFAULT_DISPLAY
        return displayManager.displays
            .filter { it.displayId != defaultDisplayId && it.state != Display.STATE_OFF }
            .map { it.displayId }
    }

    companion object {
        const val FEATURE_XR_API_SPATIAL = "android.software.xr.api.spatial"
    }
}
