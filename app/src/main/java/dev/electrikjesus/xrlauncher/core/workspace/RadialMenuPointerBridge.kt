package dev.electrikjesus.xrlauncher.core.workspace

/**
 * Bridge from companion / FPS pointer clicks into the visible [RadialMenuView]
 * without forcing Compose click listeners to dismiss-only.
 */
object RadialMenuPointerBridge {
    @Volatile
    var onActivate: (() -> Boolean)? = null
}
