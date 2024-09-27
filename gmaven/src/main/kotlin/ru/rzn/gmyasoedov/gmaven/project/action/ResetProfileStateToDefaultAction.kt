package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.containers.ContainerUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.view.ProfileNodes
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService

class ResetProfileStateToDefaultAction :
    ExternalSystemNodeAction<AbstractExternalEntityData>(AbstractExternalEntityData::class.java), DumbAware {

    init {
        getTemplatePresentation().text = GBundle.message("gmaven.action.reset.profile.text")
        getTemplatePresentation().setDescription(GBundle.message("gmaven.action.reset.profile.text"))
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        if (!super.isEnabled(e)) return false
        if (getSystemId(e) != GMavenConstants.SYSTEM_ID) return false
        val selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (selectedNodes == null || selectedNodes.size != 1) return false
        if (selectedNodes[0] is ProfileNodes) return true
        val externalData = selectedNodes[0].data
        return externalData is ProjectData || externalData is ProfileNodes
    }

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        externalData: AbstractExternalEntityData,
        e: AnActionEvent
    ) {
        val selectedNode = ContainerUtil.getFirstItem(e.getData(ExternalSystemDataKeys.SELECTED_NODES)) ?: return
        val profileNodes = getProfileNodes(selectedNode) ?: return
        val stateService = ProjectProfilesStateService.getInstance(project)
        profileNodes.profiles.forEach {
            stateService.state.mapping.remove(it.data?.stateKey)
        }
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)
    }

    private fun getProfileNodes(selectedNode: ExternalSystemNode<Any>): ProfileNodes? {
        if (selectedNode is ProjectNode) {
            return selectedNode.children.find { it is ProfileNodes } as? ProfileNodes
        }
        if (selectedNode is ProfileNodes) return selectedNode
        return null
    }
}