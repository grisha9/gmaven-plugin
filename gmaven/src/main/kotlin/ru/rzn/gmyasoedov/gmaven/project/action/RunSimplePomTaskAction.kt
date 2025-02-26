package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.execution.Executor
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle


class RunSimplePomTaskAction :
    ExternalSystemNodeAction<AbstractExternalEntityData>(AbstractExternalEntityData::class.java), DumbAware {
    private val delegateAction = Executor.EXECUTOR_EXTENSION_NAME
        .findExtension(DefaultRunExecutor::class.java)
        ?.let { RunContextAction(it) }

    init {
        templatePresentation.icon = AllIcons.Actions.Execute
        templatePresentation.text = GBundle.message("gmaven.action.task.single.pom", "")
        templatePresentation.setDescription(GBundle.message("gmaven.action.task.single.pom.description"))
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        if (!super.isEnabled(e)) return false
        if (delegateAction == null) return false
        val systemId = getSystemId(e)
        if (systemId != GMavenConstants.SYSTEM_ID) return false
        val selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (selectedNodes == null || selectedNodes.size != 1) return false
        val externalData = selectedNodes[0].data
        return externalData is TaskData
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val systemId = getSystemId(e)
        if (systemId != GMavenConstants.SYSTEM_ID) return
        val selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        val taskNode = selectedNodes.takeIf { it?.size == 1 }?.get(0) ?: return
        if (taskNode.data !is TaskData) return
        val moduleName = getModuleName(taskNode) ?: return
        e.presentation.text = GBundle.message("gmaven.action.task.single.pom", moduleName)
    }

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        externalData: AbstractExternalEntityData,
        e: AnActionEvent
    ) {
        delegateAction ?: return
        delegateAction::class.members.firstOrNull { it.name == "actionPerformed" }?.call(delegateAction, e)
    }

    private fun getModuleName(taskNode: ExternalSystemNode<*>): String? {
        var currentNode = taskNode
        while (currentNode.parent != null) {
            val newParent = currentNode.parent as? ExternalSystemNode<*> ?: return null
            if (newParent.data is ModuleData) {
                return (newParent.data as ModuleData).moduleName
            } else if (newParent.data is ProjectData) {
                return (newParent.data as ProjectData).externalName
            }
            currentNode = newParent
        }
        return null
    }
}