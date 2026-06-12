package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.launch

/** Persists panel layout changes from the glasses workspace (save on drag/preset apply). */
class PanelLayoutSaver(
    private val repository: WorkspaceRepository,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    fun save(panels: List<PanelState>) {
        scope.launch {
            repository.updatePanels(panels)
        }
    }
}

@Composable
fun rememberPanelLayoutSaver(repository: WorkspaceRepository): PanelLayoutSaver {
    val scope = rememberCoroutineScope()
    return androidx.compose.runtime.remember(repository, scope) {
        PanelLayoutSaver(repository, scope)
    }
}

/** @deprecated Use [rememberPanelLayoutSaver] — debounce removed; saves were snapping back before flush. */
@Composable
fun rememberDebouncedPanelSaver(repository: WorkspaceRepository): PanelLayoutSaver =
    rememberPanelLayoutSaver(repository)
