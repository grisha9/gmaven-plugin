package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.execution.Executor
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.TaskNode
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle

class RunTasksAction : ExternalSystemAction() {
    private val delegateAction: RunContextAction?

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val nodes = getSelectedNodes(e)
        if (nodes.size < 2) return
        val selectedTasks = getSelectedTasks(nodes)
        if (selectedTasks.size < 2) return
        val tasks = selectedTasks.mapNotNull { it.data?.name }
        if (tasks.isEmpty()) return
        e.presentation.text = GBundle.message("gmaven.action.run.tasks", tasks.toString())
    }

    override fun isVisible(e: AnActionEvent): Boolean {
        return delegateAction != null && hasManyTasks(e)
    }

    private fun hasManyTasks(e: AnActionEvent): Boolean {
        val selectedNodes = getSelectedNodes(e)
        return selectedNodes.size > 1 && getSelectedTasks(selectedNodes).size > 1
    }

    private fun getSelectedTasks(nodes: List<ExternalSystemNode<Any>>): List<TaskNode> {
        return nodes.filterIsInstance<TaskNode>()
    }

    private fun getSelectedNodes(e: AnActionEvent): List<ExternalSystemNode<Any>> {
        return e.getData(ExternalSystemDataKeys.SELECTED_NODES) ?: return emptyList()
    }

    override fun actionPerformed(e: AnActionEvent) {
        delegateAction?.actionPerformed(e)
    }

    init {
        templatePresentation.icon = AllIcons.Actions.Execute
        delegateAction = Executor.EXECUTOR_EXTENSION_NAME
            .findExtension(DefaultRunExecutor::class.java)
            ?.let { RunContextAction(it) }
    }
}
