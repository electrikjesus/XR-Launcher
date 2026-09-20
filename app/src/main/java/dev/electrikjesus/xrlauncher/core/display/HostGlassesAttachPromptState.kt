package dev.electrikjesus.xrlauncher.core.display

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Session memory for the host “what to show on glasses?” prompt.
 * Survives Compose recomposition; cleared when the host immersive session ends.
 */
object HostGlassesAttachPromptState {
    private val _dismissedDisplayIds = MutableStateFlow<Set<Int>>(emptySet())
    val dismissedDisplayIdsFlow: StateFlow<Set<Int>> = _dismissedDisplayIds.asStateFlow()
    val dismissedDisplayIds: Set<Int> get() = _dismissedDisplayIds.value

    fun dismiss(displayIds: Collection<Int>) {
        if (displayIds.isEmpty()) return
        _dismissedDisplayIds.value = dismissedDisplayIds + displayIds
    }

    fun prune(secondaryDisplayIds: List<Int>) {
        val next = HostGlassesAttachLogic.pruneDismissed(dismissedDisplayIds, secondaryDisplayIds)
        if (next != dismissedDisplayIds) {
            _dismissedDisplayIds.value = next
        }
    }

    fun clear() {
        _dismissedDisplayIds.value = emptySet()
    }
}
