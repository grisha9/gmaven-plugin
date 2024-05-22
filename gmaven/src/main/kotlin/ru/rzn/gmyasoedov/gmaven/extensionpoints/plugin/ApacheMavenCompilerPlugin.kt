package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.pom.java.LanguageLevel
import com.jetbrains.rd.util.getOrCreate
import org.jdom.Element
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.serverapi.model.*
import java.nio.file.Path

class ApacheMavenCompilerPlugin : MavenCompilerFullImportPlugin {
    override fun getGroupId() = "org.apache.maven.plugins"

    override fun getArtifactId() = "maven-compiler-plugin"

    override fun getAnnotationProcessorTagName() = "annotationProcessorPaths"

    override fun priority() = 10

    override fun getCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): CompilerData {
        val compilerProp = getCompilerProp(plugin.body, project, contextElementMap)
        return toCompilerData(compilerProp, project, plugin, localRepositoryPath, contextElementMap)
    }

    override fun getJavaCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        compilerData: CompilerData,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): MainJavaCompilerData {
        val configuration = plugin.body.configuration ?: return MainJavaCompilerData.createDefault()
        val pluginConfiguration = getElement(configuration, contextElementMap)
        val compilerId = pluginConfiguration.getChild("compilerId")?.textTrim
            ?: return MainJavaCompilerData.createDefault()
        val dependenciesPath = getDependenciesPath(plugin.body.dependencies, localRepositoryPath)
        return MainJavaCompilerData.create(compilerId, dependenciesPath, compilerData.arguments)
    }

    private fun toCompilerData(
        compilerProp: CompilerProp,
        mavenProject: MavenProject,
        plugin: MavenPlugin,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): CompilerData {
        var source: LanguageLevel? = compilerProp.release
        var target: LanguageLevel? = compilerProp.release
        var testSource: LanguageLevel? = compilerProp.testRelease
        var testTarget: LanguageLevel? = compilerProp.testRelease

        if (source == null) {
            source = compilerProp.source
        }
        if (target == null) {
            target = compilerProp.target
        }
        if (source == null || target == null) {
            val defaultCompilerData = getDefaultCompilerData(plugin, localRepositoryPath)
            if (source == null) {
                source = defaultCompilerData.sourceLevel
            }
            if (target == null) {
                target = defaultCompilerData.targetLevel
            }
        }

        if (testSource == null) {
            testSource = compilerProp.testSource ?: source
        }
        if (testTarget == null) {
            testTarget = compilerProp.testTarget ?: target
        }
        val configurationElement = plugin.body.configuration?.let { getElement(it, contextElementMap) }
        val compilerArgs = collectCompilerArgs(mavenProject, configurationElement)
        return CompilerData(source, target, testSource, testTarget, plugin.body.annotationProcessorPaths, compilerArgs)
    }

    private fun getCompilerProp(
        body: PluginBody, project: MavenProject,
        contextElementMap: MutableMap<String, Element>
    ): CompilerProp {
        val executions = body.executions
            .filter { it.phase != null && (it.phase.equals("compile") || it.phase.equals("test-compile")) }
        val compilerProp = if (executions.isEmpty()) getCompilerProp(body.configuration, contextElementMap)
        else compilerProp(executions, contextElementMap)
        fillCompilerPropFromMavenProjectProperies(compilerProp, project)
        return compilerProp
    }

    private fun compilerProp(executions: List<PluginExecution>, contextElementMap: MutableMap<String, Element>) =
        executions.asSequence().map { getCompilerProp(it.configuration, contextElementMap) }
            .reduce { acc, next -> sumCompilerProp(acc, next) }

    private fun sumCompilerProp(acc: CompilerProp, next: CompilerProp): CompilerProp {
        acc.release = maxLanguageLevel(acc.release, next.release)
        acc.source = maxLanguageLevel(acc.source, next.source)
        acc.target = maxLanguageLevel(acc.target, next.target)
        acc.testRelease = maxLanguageLevel(acc.testRelease, next.testRelease)
        acc.testSource = maxLanguageLevel(acc.testSource, next.testSource)
        acc.testTarget = maxLanguageLevel(acc.testTarget, next.testTarget)
        return acc
    }

    private fun maxLanguageLevel(first: LanguageLevel?, second: LanguageLevel?): LanguageLevel? {
        if (first == null) return second
        if (second == null) return first
        return if (first.isLessThan(second)) second else first
    }

    private fun getCompilerProp(configuration: String?, contextElementMap: MutableMap<String, Element>): CompilerProp {
        val element = configuration?.let { getElement(it, contextElementMap) }
            ?: return CompilerProp(null, null, null, null, null, null)
        return CompilerProp(
            getLanguageLevel(element.getChildTextTrim("release")),
            getLanguageLevel(element.getChildTextTrim("source")),
            getLanguageLevel(element.getChildTextTrim("target")),
            getLanguageLevel(element.getChildTextTrim("testRelease")),
            getLanguageLevel(element.getChildTextTrim("testSource")),
            getLanguageLevel(element.getChildTextTrim("testTarget")),
        )
    }

    private fun getDefaultCompilerData(plugin: MavenPlugin, localRepositoryPath: Path): CompilerData {
        val descriptor = MavenArtifactUtil.readPluginDescriptor(localRepositoryPath, plugin)
        if (descriptor == null) {
            MavenLog.LOG.warn("null descriptor $plugin")
            return CompilerData(LanguageLevel.HIGHEST, emptyList(), emptyList())
        }
        val source = LanguageLevel.parse(descriptor.myParams["source"]) ?: LanguageLevel.HIGHEST
        val target = LanguageLevel.parse(descriptor.myParams["target"]) ?: LanguageLevel.HIGHEST
        return CompilerData(source, target, source, target, emptyList(), emptyList())
    }

    private fun getDependenciesPath(dependencies: List<MavenArtifact>?, localRepositoryPath: Path): List<String> {
        return dependencies
            ?.filter { "jar".equals(it.type, true) }
            ?.map {
                MavenArtifactUtil.getArtifactNioPath(
                    localRepositoryPath, it.groupId, it.artifactId, it.version, it.type
                ).toString()
            } ?: emptyList()
    }

    private data class CompilerProp(
        var release: LanguageLevel?, var source: LanguageLevel?, var target: LanguageLevel?,
        var testRelease: LanguageLevel?, var testSource: LanguageLevel?, var testTarget: LanguageLevel?
    )

    companion object {
        fun getElement(body: String, contextElementMap: MutableMap<String, Element>): Element {
            return contextElementMap.getOrCreate(body) { MavenJDOMUtil.parseConfiguration(it) }
        }

        fun getDefaultCompilerData(mavenProject: MavenProject, defaultLanguageLevel: LanguageLevel): CompilerData {
            val compilerProp = CompilerProp(null, null, null, null, null, null)
            fillCompilerPropFromMavenProjectProperies(compilerProp, mavenProject)
            var source: LanguageLevel? = compilerProp.release
            var target: LanguageLevel? = compilerProp.release
            var testSource: LanguageLevel? = compilerProp.testRelease
            var testTarget: LanguageLevel? = compilerProp.testRelease

            if (source == null) {
                source = compilerProp.source
            }
            if (target == null) {
                target = compilerProp.target
            }
            if (testSource == null) {
                testSource = compilerProp.testSource ?: source
            }
            if (testTarget == null) {
                testTarget = compilerProp.testTarget ?: target
            }
            if (source == null || target == null || testSource == null || testTarget == null) {
                return CompilerData(defaultLanguageLevel, emptyList(), emptyList())
            }
            return CompilerData(source, target, testSource, testTarget, emptyList(), emptyList())
        }

        private fun fillCompilerPropFromMavenProjectProperies(
            compilerProp: CompilerProp,
            project: MavenProject
        ) {
            if (compilerProp.release == null) {
                compilerProp.release = getLanguageLevel(project.properties["maven.compiler.release"])
            }
            if (compilerProp.source == null) {
                compilerProp.source = getLanguageLevel(project.properties["maven.compiler.source"])
            }
            if (compilerProp.target == null) {
                compilerProp.target = getLanguageLevel(project.properties["maven.compiler.target"])
            }
            if (compilerProp.testRelease == null) {
                compilerProp.testRelease = getLanguageLevel(project.properties["maven.compiler.testRelease"])
            }
            if (compilerProp.testSource == null) {
                compilerProp.testSource = getLanguageLevel(project.properties["maven.compiler.testSource"])
            }
            if (compilerProp.testTarget == null) {
                compilerProp.testTarget = getLanguageLevel(project.properties["maven.compiler.testTarget"])
            }
        }
    }
}