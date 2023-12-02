package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings

class SkipTestsAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return MavenSettings.getInstance(project).isSkipTests
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        val settings = MavenSettings.getInstance(project)
        settings.isSkipTests = !settings.isSkipTests
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = (e.getData(EXTERNAL_SYSTEM_ID) == GMavenConstants.SYSTEM_ID)
    }

    init {
        templatePresentation.icon = AllIcons.RunConfigurations.ShowIgnored
        templatePresentation.text = GBundle.message("gmaven.action.skip.tests")
    }
}