package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings

class OfflineModeAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return MavenSettings.getInstance(project).isOfflineMode
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        val settings = MavenSettings.getInstance(project)
        settings.isOfflineMode = !settings.isOfflineMode
    }

    init {
        templatePresentation.icon = AllIcons.Actions.OfflineMode
        templatePresentation.text = GBundle.message("gmaven.action.offline")
    }
}