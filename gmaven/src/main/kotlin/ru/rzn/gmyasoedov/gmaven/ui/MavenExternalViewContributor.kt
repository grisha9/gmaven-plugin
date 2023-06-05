package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ExternalSystemViewContributor
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.DependencyAnalyzerData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.LifecycleData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.view.DependencyAnalyzerNode
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.view.LifecycleNodes
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.view.PluginNodes
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.view.ProfileNodes

class MavenExternalViewContributor : ExternalSystemViewContributor() {
    override fun getSystemId() = GMavenConstants.SYSTEM_ID

    override fun getKeys(): List<Key<*>> = listOf(
        LifecycleData.KEY, PluginData.KEY, ProfileData.KEY, DependencyAnalyzerData.KEY
    )

    override fun createNodes(
        externalProjectsView: ExternalProjectsView,
        dataNodes: MultiMap<Key<*>?, DataNode<*>?>
    ): List<ExternalSystemNode<*>> {
        val result: MutableList<ExternalSystemNode<*>> = SmartList()

        // add profiles
        val profilesNodes = dataNodes[ProfileData.KEY]
        if (!profilesNodes.isEmpty()) {
            result.add(ProfileNodes(externalProjectsView, profilesNodes))
        }

        // add base lifecycle tasks
        val tasksNodes = dataNodes[LifecycleData.KEY]
        if (!tasksNodes.isEmpty()) {
            result.add(LifecycleNodes(externalProjectsView, tasksNodes))
        }

        // add plugin tasks
        val pluginsNode = dataNodes[PluginData.KEY]
        if (!pluginsNode.isEmpty()) {
            result.add(PluginNodes(externalProjectsView, replacePluginDataOnTaskData(pluginsNode)))
        }

        // add DA node
        val dependencyAnalyzerNodes = dataNodes[DependencyAnalyzerData.KEY]
        if (!dependencyAnalyzerNodes.isEmpty()) {
            result.add(DependencyAnalyzerNode(externalProjectsView))
        }
        return result
    }

    private fun replacePluginDataOnTaskData(pluginsNode: Collection<DataNode<*>?>): List<DataNode<*>?> {
        return pluginsNode.asSequence()
            .filter { it?.data is PluginData }
            .map { toTaskNode(it as DataNode<PluginData>) }
            .toList()
    }

    private fun toTaskNode(dataNode: DataNode<PluginData>): DataNode<TaskData> {
        val data = dataNode.getData()
        val taskData = TaskData(GMavenConstants.SYSTEM_ID, data.name, data.linkedExternalProjectPath, data.description)
        taskData.group = data.group
        return DataNode(ProjectKeys.TASK, taskData, dataNode.parent)
    }

    override fun getDisplayName(node: DataNode<*>): String? {
        val profileData = node.data as? ProfileData ?: return null
        return profileData.name
    }
}
