package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData

class ChangeProfileStateAction : ExternalSystemNodeAction<ProfileData>(ProfileData::class.java) {

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        externalData: ProfileData,
        e: AnActionEvent
    ) {
        externalData.nextState()
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)
    }
}