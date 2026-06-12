package dev.electrikjesus.xrlauncher.core.display

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/** Debug spike: force Jetpack XR Subspace on EXTERNAL display without spatial API. */
object SubspaceSpike {
    const val TAG = "XRLauncher/SubspaceSpike"

    /**
     * When true (debug builds only), glasses session uses [GlassesWorkspaceScreen]
     * even if `android.software.xr.api.spatial` is absent.
     */
    const val FORCE_ON_EXTERNAL_DEBUG = true

    data class Decision(
        val hasSpatialApi: Boolean,
        val forcedForSpike: Boolean,
        val preferSubspaceShell: Boolean,
    )

    fun resolvePreferSubspace(context: Context): Decision {
        val hasSpatialApi = context.packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val forced = isDebug && FORCE_ON_EXTERNAL_DEBUG
        val prefer = hasSpatialApi || forced
        val decision = Decision(
            hasSpatialApi = hasSpatialApi,
            forcedForSpike = forced,
            preferSubspaceShell = prefer,
        )
        Log.i(
            TAG,
            "resolvePreferSubspace spatialApi=$hasSpatialApi forced=$forced preferSubspace=$prefer debug=$isDebug",
        )
        return decision
    }

    fun logCompositionStage(
        stage: String,
        displayId: Int?,
        decision: Decision,
        detail: String = "",
    ) {
        Log.i(
            TAG,
            "compose stage=$stage displayId=$displayId spatialApi=${decision.hasSpatialApi} " +
                "forced=${decision.forcedForSpike} preferSubspace=${decision.preferSubspaceShell}" +
                if (detail.isNotBlank()) " $detail" else "",
        )
    }

    private const val FEATURE_XR_API_SPATIAL = "android.software.xr.api.spatial"
}
