package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.*
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenContentRoot
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution

class BuildHelperMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.mojo"

    override fun getArtifactId() = "build-helper-maven-plugin"

    override fun getContentRoots(mavenProject: MavenProject, plugin: MavenPlugin): PluginContentRoots {
        val executions = plugin.body?.executions ?: emptyList()
        val result = ArrayList<MavenContentRoot>()
        for (execution in executions) {
            when {
                execution.goals.contains("add-source") ->
                    getPathList(execution, "sources").forEach { result.add(MavenContentRoot(SOURCE, it)) }

                execution.goals.contains("add-test-source") ->
                    getPathList(execution, "sources").forEach { result.add(MavenContentRoot(TEST, it)) }

                execution.goals.contains("add-resource") ->
                    getPathList(execution, "resources").forEach { result.add(MavenContentRoot(RESOURCE, it)) }

                execution.goals.contains("add-test-resource") ->
                    getPathList(execution, "resources").forEach { result.add(MavenContentRoot(TEST_RESOURCE, it)) }
            }
        }
        return PluginContentRoots(result, emptySet())
    }

    private fun getPathList(execution: PluginExecution, paramName: String): List<String> {
        val element = MavenJDOMUtil.parseConfiguration(execution.configuration)
        val paths = ArrayList<String>()
        for (sourceElement in element.getChild(paramName)?.children ?: emptyList()) {
            val sourcePath = sourceElement.textTrim
            if (sourcePath != null && sourcePath.isNotEmpty()) {
                paths.add(sourcePath)
            }
        }
        return paths
    }
}