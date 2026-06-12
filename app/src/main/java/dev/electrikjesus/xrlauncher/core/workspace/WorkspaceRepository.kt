package dev.electrikjesus.xrlauncher.core.workspace

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.workspaceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "xrlauncher_workspace",
)

class WorkspaceRepository(private val context: Context) {
    val workspace: Flow<Workspace> = context.workspaceDataStore.data.map { prefs ->
        prefs[WORKSPACE_JSON_KEY]?.let { json ->
            runCatching { WorkspaceJson.decode(json) }.getOrElse { Workspace.default() }
        } ?: Workspace.default()
    }

    suspend fun save(workspace: Workspace) {
        context.workspaceDataStore.edit { prefs ->
            prefs[WORKSPACE_JSON_KEY] = WorkspaceJson.encode(workspace)
        }
    }

    suspend fun updatePanels(panels: List<PanelState>) {
        context.workspaceDataStore.edit { prefs ->
            val current = prefs[WORKSPACE_JSON_KEY]?.let {
                runCatching { WorkspaceJson.decode(it) }.getOrNull()
            } ?: Workspace.default()
            prefs[WORKSPACE_JSON_KEY] = WorkspaceJson.encode(current.copy(panels = panels))
        }
    }

    suspend fun toggleHotseatPin(componentKey: String) {
        context.workspaceDataStore.edit { prefs ->
            val current = prefs[WORKSPACE_JSON_KEY]?.let {
                runCatching { WorkspaceJson.decode(it) }.getOrNull()
            } ?: Workspace.default()
            val pins = current.hotseatPins.toMutableList()
            if (componentKey in pins) {
                pins.remove(componentKey)
            } else {
                pins.add(componentKey)
            }
            prefs[WORKSPACE_JSON_KEY] = WorkspaceJson.encode(current.copy(hotseatPins = pins))
        }
    }

    companion object {
        private val WORKSPACE_JSON_KEY = stringPreferencesKey("workspace_json")
    }
}
