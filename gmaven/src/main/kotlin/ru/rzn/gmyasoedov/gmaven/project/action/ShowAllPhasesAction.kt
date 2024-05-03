package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings

class ShowAllPhasesAction :
    ExternalSystemNodeAction<ProjectData>(ProjectData::class.java), DumbAware {

    init {
        getTemplatePresentation().text = GBundle.message("gmaven.action.show.all.phases")
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val presentation = e.presentation
        val showAllPhases = MavenSettings.getInstance(project).isShowAllPhases
        if (showAllPhases) {
            presentation.icon = AllIcons.Actions.SetDefault
        } else {
            presentation.icon = null
        }
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        if (!super.isEnabled(e)) return false
        val systemId = getSystemId(e)
        if (systemId != GMavenConstants.SYSTEM_ID) return false
        val selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (selectedNodes == null || selectedNodes.size != 1) return false
        val externalData = selectedNodes[0].data
        return externalData is ProjectData
    }

    override fun perform(
        project: Project, projectSystemId: ProjectSystemId, projectData: ProjectData, e: AnActionEvent
    ) {
        MavenSettings.getInstance(project).isShowAllPhases = !MavenSettings.getInstance(project).isShowAllPhases
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)
    }
}