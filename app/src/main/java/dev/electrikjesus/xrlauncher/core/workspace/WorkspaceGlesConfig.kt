package dev.electrikjesus.xrlauncher.core.workspace

/** Runtime toggles for GLES workspace presentation. */
object WorkspaceGlesConfig {
    /** Capture Compose panels and render them as GL textures on the cylinder. */
    var texturedPanelsEnabled: Boolean = false

    /** Hide Compose panel chrome when GL textures are active (interaction stays on Compose). */
    var hideComposePanelsWhenGles: Boolean = false

    /** Draw wireframe cylinder and slot guides (off by default — use Compose panels as source of truth). */
    var showGuideWireframe: Boolean = false

    /** Map the device wallpaper onto the inner cylinder wall (off by default until aligned with Compose layout). */
    var showWallpaperCylinder: Boolean = false
}
