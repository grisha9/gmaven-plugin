package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.SOURCE
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.TEST
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenContentRoot
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil.JDOM_ELEMENT_EMPTY
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import java.nio.file.Path

object GroovyAbstractMavenPlugin {

    fun getContentRoots(mavenProject: MavenProject, plugin: MavenPlugin): PluginContentRoots {
        val executions = plugin.body?.executions ?: emptyList()
        val result = ArrayList<MavenContentRoot>()
        val excluded = HashSet<String>(4)

        var mainConfiguration: String? = null
        var testConfiguration: String? = null
        var generatedConfiguration: String? = null
        var generatedTestConfiguration: String? = null
        for (execution in executions) {
            if (execution.goals.contains("compile")) {
                mainConfiguration = execution.configuration
            }
            if (execution.goals.contains("testCompile") || execution.goals.contains("compileTests")) {
                testConfiguration = execution.configuration
            }
            if (execution.goals.contains("generateStubs")) {
                generatedConfiguration = execution.configuration
            }
            if (execution.goals.contains("generateTestStubs")) {
                generatedTestConfiguration = execution.configuration
            }
        }
        getPathList(mavenProject, mainConfiguration, false).forEach { result.add(MavenContentRoot(SOURCE, it)) }
        getPathList(mavenProject, testConfiguration, true).forEach { result.add(MavenContentRoot(TEST, it)) }
        excluded.add(getExcludedPath(mavenProject, generatedConfiguration))
        excluded.add(getExcludedPath(mavenProject, generatedTestConfiguration))
        return PluginContentRoots(result, excluded)
    }

    private fun getPathList(mavenProject: MavenProject, configuration: String?, isTest: Boolean): List<String> {
        val element = MavenJDOMUtil.parseConfiguration(configuration)
        if (element == JDOM_ELEMENT_EMPTY) {
            return getDefaultPath(mavenProject, isTest)
        }
        val dirs = MavenJDOMUtil.findChildrenValuesByPath(element, "sources", "fileset.directory")
        if (dirs.isEmpty()) {
            return getDefaultPath(mavenProject, isTest)
        }
        return dirs
    }

    private fun getDefaultPath(mavenProject: MavenProject, isTest: Boolean): List<String> {
        val sourceFolderName = if (isTest) "test" else "main"
        return listOf(Path.of(mavenProject.basedir, "src", sourceFolderName, "groovy").toString())
    }

    private fun getExcludedPath(mavenProject: MavenProject, configuration: String?): String {
        val element = MavenJDOMUtil.parseConfiguration(configuration)
        if (element == JDOM_ELEMENT_EMPTY) return getDefaultExcludedDir(mavenProject)
        return MavenJDOMUtil.findChildValueByPath(element, "outputDirectory", null)
            ?: return getDefaultExcludedDir(mavenProject)
    }

    private fun getDefaultExcludedDir(mavenProject: MavenProject) =
        MavenUtils.getGeneratedSourcesDirectory(mavenProject.buildDirectory, false).resolve("groovy-stubs").toString()
}