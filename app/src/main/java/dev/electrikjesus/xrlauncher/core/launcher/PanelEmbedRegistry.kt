package dev.electrikjesus.xrlauncher.core.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import dev.electrikjesus.xrlauncher.core.workspace.PanelState

/**
 * Phase 3.3 — manages [ActivityPanelEntity] instances per workspace panel slot.
 * Requires a live XR [Session] (Tier 3 / spatial API).
 */
class PanelEmbedRegistry(
    val session: Session,
) {
    private val entities = mutableMapOf<String, ActivityPanelEntity>()

    fun canEmbed(): Boolean =
        session.scene.spatialCapabilities.contains(SpatialCapability.EMBED_ACTIVITY)

    fun embedLaunch(panel: PanelState, componentName: ComponentName): Boolean {
        if (!canEmbed()) return false
        return runCatching {
            val entity = entities.getOrPut(panel.id) {
                ActivityPanelEntity.create(
                    session = session,
                    pixelDimensions = DEFAULT_PANEL_SIZE,
                    name = panel.id,
                )
            }
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            entity.startActivity(intent)
            Log.i(TAG, "Embedded ${componentName.flattenToShortString()} in panel ${panel.id}")
            true
        }.getOrElse { error ->
            Log.w(TAG, "Embed launch failed for panel ${panel.id}", error)
            false
        }
    }

    fun dispose(panelId: String) {
        entities.remove(panelId)?.parent = null
    }

    fun disposeAll() {
        entities.keys.toList().forEach(::dispose)
    }

    companion object {
        private const val TAG = "XRLauncher/PanelEmbed"
        private val DEFAULT_PANEL_SIZE = IntSize2d(960, 540)

        fun fromActivity(activity: Activity): PanelEmbedRegistry? =
            when (val result = Session.create(activity)) {
                is SessionCreateSuccess -> PanelEmbedRegistry(result.session)
                else -> {
                    Log.d(TAG, "No XR session for ${activity.javaClass.simpleName}: $result")
                    null
                }
            }
    }
}
