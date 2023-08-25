package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.*
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin.parseConfiguration
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenContentRoot
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution

class BuildHelperMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.mojo"

    override fun getArtifactId() = "build-helper-maven-plugin"

    override fun getContentRoots(
        mavenProject: MavenProject, plugin: MavenPlugin, context: MavenProjectResolver.ProjectResolverContext
    ): PluginContentRoots {
        val executions = plugin.body?.executions ?: emptyList()
        val result = ArrayList<MavenContentRoot>()
        for (execution in executions) {
            when {
                execution.goals.contains("add-source") ->
                    getPathList(execution, "sources", context).forEach { result.add(MavenContentRoot(SOURCE, it)) }

                execution.goals.contains("add-test-source") ->
                    getPathList(execution, "sources", context).forEach { result.add(MavenContentRoot(TEST, it)) }

                execution.goals.contains("add-resource") ->
                    getPathList(execution, "resources", context).forEach { result.add(MavenContentRoot(RESOURCE, it)) }

                execution.goals.contains("add-test-resource") ->
                    getPathList(execution, "resources", context).forEach { result.add(MavenContentRoot(TEST_RESOURCE, it)) }
            }
        }
        return PluginContentRoots(result, emptySet())
    }

    private fun getPathList(
        execution: PluginExecution, paramName: String, context: MavenProjectResolver.ProjectResolverContext
    ): List<String> {
        val element = parseConfiguration(execution.configuration, context)
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