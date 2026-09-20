package dev.electrikjesus.xrlauncher.core.workspace

/** User-selectable workspace backdrop (Phase 2.17). */
enum class WorkspaceWallpaperChoice {
    SYSTEM,
    GRADIENT_TWILIGHT,
    GRADIENT_AURORA,
    GRADIENT_EMISSIVE,
    /** Equirectangular HDRI from Poly Haven (CC0), sampled as a sphere map. */
    POLY_HAVEN,
    ;

    companion object {
        fun fromPersisted(raw: String?): WorkspaceWallpaperChoice =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}
