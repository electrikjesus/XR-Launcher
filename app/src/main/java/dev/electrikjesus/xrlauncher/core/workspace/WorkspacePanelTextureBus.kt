package dev.electrikjesus.xrlauncher.core.workspace

import android.graphics.Bitmap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** Snapshot of a panel bitmap positioned on the workspace cylinder. */
data class PanelTextureSnapshot(
    val panelId: String,
    val centerXNorm: Float,
    val centerYNorm: Float,
    val widthNorm: Float,
    val heightNorm: Float,
    val bitmap: Bitmap,
    val generation: Long,
)

/**
 * Main-thread registry of panel textures uploaded to [dev.electrikjesus.xrlauncher.ui.spatial.gles.CylinderGlRenderer].
 */
object WorkspacePanelTextureBus {
    private val snapshots = LinkedHashMap<String, PanelTextureSnapshot>()
    private val renderCallbacks = CopyOnWriteArrayList<() -> Unit>()
    private val generation = AtomicLong(0)

    fun registerRenderCallback(callback: () -> Unit) {
        renderCallbacks += callback
    }

    fun unregisterRenderCallback(callback: () -> Unit) {
        renderCallbacks -= callback
    }

    fun update(
        panelId: String,
        centerXNorm: Float,
        centerYNorm: Float,
        widthNorm: Float,
        heightNorm: Float,
        bitmap: Bitmap,
    ) {
        val gen = generation.incrementAndGet()
        snapshots[panelId] = PanelTextureSnapshot(
            panelId = panelId,
            centerXNorm = centerXNorm,
            centerYNorm = centerYNorm,
            widthNorm = widthNorm,
            heightNorm = heightNorm,
            bitmap = bitmap,
            generation = gen,
        )
        renderCallbacks.forEach { it.invoke() }
    }

    fun remove(panelId: String) {
        if (snapshots.remove(panelId) != null) {
            renderCallbacks.forEach { it.invoke() }
        }
    }

    fun snapshot(): List<PanelTextureSnapshot> = snapshots.values.toList()

    fun clear() {
        snapshots.clear()
        renderCallbacks.forEach { it.invoke() }
    }
}
