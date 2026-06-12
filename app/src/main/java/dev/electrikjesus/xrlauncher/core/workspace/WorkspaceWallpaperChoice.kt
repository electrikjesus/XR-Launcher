package dev.electrikjesus.xrlauncher.core.workspace

/** User-selectable workspace backdrop (Phase 2.17). */
enum class WorkspaceWallpaperChoice {
    SYSTEM,
    GRADIENT_TWILIGHT,
    GRADIENT_AURORA,
    GRADIENT_EMISSIVE,
    ;

    companion object {
        fun fromPersisted(raw: String?): WorkspaceWallpaperChoice =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}
