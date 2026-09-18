package dev.electrikjesus.xrlauncher.core.workspace

import android.graphics.Bitmap
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class DeskIconSnapshot(
    val componentKey: String,
    val bitmap: Bitmap,
    val generation: Long,
)

/** Main-thread registry of GLES desk icons and their bitmaps. */
object DeskIconTextureBus {
    private val renderCallbacks = CopyOnWriteArrayList<() -> Unit>()
    private val generation = AtomicLong(0)
    private val iconsRef = AtomicReference<List<HomeSpaceDesk.Icon>>(emptyList())
    private val snapshotsRef = AtomicReference<List<DeskIconSnapshot>>(emptyList())
    private val hoveredKeyRef = AtomicReference<String?>(null)

    fun registerRenderCallback(callback: () -> Unit) {
        renderCallbacks += callback
    }

    fun unregisterRenderCallback(callback: () -> Unit) {
        renderCallbacks -= callback
    }

    fun set(icons: List<HomeSpaceDesk.Icon>, snapshots: List<DeskIconSnapshot>) {
        iconsRef.set(icons)
        snapshotsRef.set(snapshots)
        generation.incrementAndGet()
        renderCallbacks.forEach { it.invoke() }
    }

    fun setIcons(icons: List<HomeSpaceDesk.Icon>) {
        iconsRef.set(icons)
        generation.incrementAndGet()
        renderCallbacks.forEach { it.invoke() }
    }

    fun setHoveredKey(key: String?) {
        if (hoveredKeyRef.get() == key) return
        hoveredKeyRef.set(key)
        renderCallbacks.forEach { it.invoke() }
    }

    fun icons(): List<HomeSpaceDesk.Icon> = iconsRef.get()

    fun snapshots(): List<DeskIconSnapshot> = snapshotsRef.get()

    fun hoveredKey(): String? = hoveredKeyRef.get()

    fun clear() {
        iconsRef.set(emptyList())
        snapshotsRef.set(emptyList())
        hoveredKeyRef.set(null)
        renderCallbacks.forEach { it.invoke() }
    }
}
