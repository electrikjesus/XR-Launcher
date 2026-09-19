package dev.electrikjesus.xrlauncher.core.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-engine dialogs for immersive Home Space (host / glasses), not full Activities. */
enum class HomeSpaceDialog {
    NONE,
    SETTINGS,
}

object HomeSpaceDialogState {
    const val EDIT_TEXTURE_ID = "__xr_dialog_edit__"
    const val SETTINGS_TEXTURE_ID = "__xr_dialog_settings__"

    private val _dialog = MutableStateFlow(HomeSpaceDialog.NONE)
    val dialogFlow: StateFlow<HomeSpaceDialog> = _dialog.asStateFlow()
    val dialog: HomeSpaceDialog get() = _dialog.value

    fun openSettings() {
        _dialog.value = HomeSpaceDialog.SETTINGS
    }

    fun close() {
        _dialog.value = HomeSpaceDialog.NONE
    }

    fun toggleSettings() {
        _dialog.value = if (_dialog.value == HomeSpaceDialog.SETTINGS) {
            HomeSpaceDialog.NONE
        } else {
            HomeSpaceDialog.SETTINGS
        }
    }

    fun isDialogTexture(panelId: String): Boolean =
        panelId == EDIT_TEXTURE_ID || panelId == SETTINGS_TEXTURE_ID
}
