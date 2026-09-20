package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LauncherContextMenuTarget {
    data class App(
        val app: LaunchableApp,
        val isPinned: Boolean,
    ) : LauncherContextMenuTarget()

    data class Panel(
        val panelId: String,
        val kind: PanelKind,
    ) : LauncherContextMenuTarget()

    data object Desktop : LauncherContextMenuTarget()
}

data class LauncherContextMenuRequest(
    val target: LauncherContextMenuTarget,
    /** Normalized anchor (0..1) for menu placement. */
    val anchorX: Float,
    val anchorY: Float,
    /** Sphere hit under the pointer when opening a Desktop menu (Add widget place). */
    val deskYawDeg: Float? = null,
    val deskPitchDeg: Float? = null,
)

/** Shared open context menu state for companion right-click and icon long-press. */
object LauncherContextMenuState {
    private val _request = MutableStateFlow<LauncherContextMenuRequest?>(null)
    val request: StateFlow<LauncherContextMenuRequest?> = _request.asStateFlow()

    val isOpen: Boolean get() = _request.value != null

    fun open(request: LauncherContextMenuRequest) {
        _request.value = request
    }

    fun openApp(
        app: LaunchableApp,
        isPinned: Boolean,
        anchorX: Float,
        anchorY: Float,
    ) {
        open(
            LauncherContextMenuRequest(
                target = LauncherContextMenuTarget.App(app, isPinned),
                anchorX = anchorX.coerceIn(0f, 1f),
                anchorY = anchorY.coerceIn(0f, 1f),
            ),
        )
    }

    fun openPanel(
        panelId: String,
        kind: PanelKind,
        anchorX: Float,
        anchorY: Float,
    ) {
        open(
            LauncherContextMenuRequest(
                target = LauncherContextMenuTarget.Panel(panelId, kind),
                anchorX = anchorX.coerceIn(0f, 1f),
                anchorY = anchorY.coerceIn(0f, 1f),
            ),
        )
    }

    fun openDesktop(
        anchorX: Float,
        anchorY: Float,
        deskYawDeg: Float? = null,
        deskPitchDeg: Float? = null,
    ) {
        open(
            LauncherContextMenuRequest(
                target = LauncherContextMenuTarget.Desktop,
                anchorX = anchorX.coerceIn(0f, 1f),
                anchorY = anchorY.coerceIn(0f, 1f),
                deskYawDeg = deskYawDeg,
                deskPitchDeg = deskPitchDeg,
            ),
        )
    }

    fun dismiss() {
        _request.value = null
    }
}
