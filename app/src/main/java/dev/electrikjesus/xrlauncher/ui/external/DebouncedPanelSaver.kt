package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PANEL_SAVE_DEBOUNCE_MS = 400L

/** Debounces panel layout writes while dragging on the glasses workspace. */
class DebouncedPanelSaver(
    private val repository: WorkspaceRepository,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    private var saveJob: Job? = null

    fun save(panels: List<PanelState>) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(PANEL_SAVE_DEBOUNCE_MS)
            repository.updatePanels(panels)
        }
    }
}

@Composable
fun rememberDebouncedPanelSaver(repository: WorkspaceRepository): DebouncedPanelSaver {
    val scope = rememberCoroutineScope()
    return androidx.compose.runtime.remember(repository, scope) {
        DebouncedPanelSaver(repository, scope)
    }
}
