package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.SnapshotUpdateType

class UpdateSnapshotAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        val settings = MavenSettings.getInstance(project).linkedProjectsSettings.firstOrNull() ?: return false
        updateState(settings.snapshotUpdateType)
        return settings.snapshotUpdateType != SnapshotUpdateType.DEFAULT
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        val settings = MavenSettings.getInstance(project).linkedProjectsSettings.firstOrNull() ?: return
        val currentType = settings.snapshotUpdateType
        val ordinal = currentType.ordinal
        val next = SnapshotUpdateType.values()[(ordinal + 1) % SnapshotUpdateType.values().size]
        MavenSettings.getInstance(project).linkedProjectsSettings.forEach { it.snapshotUpdateType = next }
        updateState(next)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = (e.getData(EXTERNAL_SYSTEM_ID) == GMavenConstants.SYSTEM_ID)
    }

    init {
        templatePresentation.text = GBundle.message("gmaven.action.snapshot")
    }

    private fun updateState(type: SnapshotUpdateType) {
        when (type) {
            SnapshotUpdateType.DEFAULT -> templatePresentation.icon = AllIcons.Actions.FindEntireFile
            SnapshotUpdateType.NEVER -> templatePresentation.icon = AllIcons.General.InspectionsTrafficOff
            else -> templatePresentation.icon = AllIcons.Actions.ForceRefresh
        }
    }
}