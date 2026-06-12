package dev.electrikjesus.xrlauncher.core.workspace

/** Runtime toggles for GLES workspace presentation. */
object WorkspaceGlesConfig {
    /** Capture Compose panels and render them as GL textures on the cylinder. */
    var texturedPanelsEnabled: Boolean = false

    /** Hide Compose panel chrome when GL textures are active (interaction stays on Compose). */
    var hideComposePanelsWhenGles: Boolean = false

    /** Draw wireframe cylinder and slot guides. */
    var showGuideWireframe: Boolean = true

    /** Map the device wallpaper onto the inner cylinder wall (preferred backdrop). */
    var showWallpaperCylinder: Boolean = true
}
