package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import ru.rzn.gmyasoedov.gmaven.project.storePath
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution

class BuildHelperMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.mojo"

    override fun getArtifactId() = "build-helper-maven-plugin"

    override fun populateModuleData(project: MavenProject, plugin: MavenPlugin, data: DataNode<ModuleData>) {
        val executions = plugin.body?.executions ?: emptyList()
        val contentRootData = data.children
            .filter { it.data is ContentRootData }
            .map { it.data as ContentRootData }
            .firstOrNull() ?: return
        for (execution in executions) {
            when {
                execution.goals.contains("add-source") -> {
                    val paths = getPathList(execution, "sources")
                    storePath(paths, contentRootData, ExternalSystemSourceType.SOURCE)
                }
                execution.goals.contains("add-test-source") -> {
                    val paths = getPathList(execution, "sources")
                    storePath(paths, contentRootData, ExternalSystemSourceType.TEST)
                }
                execution.goals.contains("add-resource") -> {
                    val paths = getPathList(execution, "resources")
                    storePath(paths, contentRootData, ExternalSystemSourceType.RESOURCE)
                }
                execution.goals.contains("add-test-resource") -> {
                    val paths = getPathList(execution, "resources")
                    storePath(paths, contentRootData, ExternalSystemSourceType.TEST_RESOURCE)
                }
            }
        }
    }

    private fun getPathList(execution: PluginExecution, paramName: String): ArrayList<String> {
        val element = MavenJDOMUtil.parseConfiguration(execution.configuration)
        val paths = ArrayList<String>()
        for (sourceElement in element.getChild(paramName)?.children ?: emptyList()) {
            val sourcePath = sourceElement.textTrim
            if (sourcePath != null) {
                paths.add(sourcePath)
            }
        }
        return paths
    }
}