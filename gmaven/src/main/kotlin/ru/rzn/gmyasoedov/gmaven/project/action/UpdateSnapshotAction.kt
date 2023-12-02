package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.SnapshotUpdateType

class UpdateSnapshotAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        val settings = MavenSettings.getInstance(project).linkedProjectsSettings.firstOrNull() ?: return false
        updateState(settings.snapshotUpdateType, e.presentation)
        return settings.snapshotUpdateType != SnapshotUpdateType.DEFAULT
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        val settings = MavenSettings.getInstance(project).linkedProjectsSettings.firstOrNull() ?: return
        val currentType = settings.snapshotUpdateType
        val ordinal = currentType.ordinal
        val next = SnapshotUpdateType.values()[(ordinal + 1) % SnapshotUpdateType.values().size]
        MavenSettings.getInstance(project).linkedProjectsSettings.forEach { it.snapshotUpdateType = next }
        updateState(next, e.presentation)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = (e.getData(EXTERNAL_SYSTEM_ID) == GMavenConstants.SYSTEM_ID)
    }

    private fun updateState(type: SnapshotUpdateType, presentation: Presentation) {
        when (type) {
            SnapshotUpdateType.DEFAULT -> {
                presentation.icon = AllIcons.Actions.FindEntireFile
                presentation.text = "Update Snapshots: Default"
            }
            SnapshotUpdateType.NEVER -> {
                presentation.icon = AllIcons.General.InspectionsTrafficOff
                presentation.text = "Update Snapshots: Never (-nsu)"
            }
            else -> {
                presentation.icon = AllIcons.Actions.ForceRefresh
                presentation.text = "Update Snapshots: Force (-U)"
            }
        }
    }
}