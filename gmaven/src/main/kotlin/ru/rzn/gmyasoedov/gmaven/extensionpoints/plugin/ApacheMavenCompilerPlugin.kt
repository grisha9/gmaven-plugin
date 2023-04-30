package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.java.LanguageLevel
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.PluginBody
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution
import java.nio.file.Path

class ApacheMavenCompilerPlugin : MavenCompilerFullImportPlugin {
    override fun getGroupId() = "org.apache.maven.plugins"

    override fun getArtifactId() = "maven-compiler-plugin"

    override fun getCompilerData(project: MavenProject, plugin: MavenPlugin, localRepositoryPath: Path): CompilerData {
        val compilerProp = getCompilerProp(plugin.body, project)
        return toCompilerData(compilerProp, plugin, localRepositoryPath)
    }

    private fun toCompilerData(
        compilerProp: CompilerProp,
        plugin: MavenPlugin,
        localRepositoryPath: Path
    ): CompilerData {
        val isReleaseEnabled = StringUtil.compareVersionNumbers(plugin.version, "3.6") >= 0
        var source: LanguageLevel? = null
        var target: LanguageLevel? = null
        var testSource: LanguageLevel? = null
        var testTarget: LanguageLevel? = null
        if (isReleaseEnabled) {
            source = compilerProp.release
            target = compilerProp.release;
            testSource = compilerProp.testRelease
            testTarget = compilerProp.testRelease
        }
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
        return CompilerData(source, target, testSource, testTarget, emptyList())
    }

    private fun getCompilerProp(body: PluginBody, project: MavenProject): CompilerProp {
        val executions = body.executions.filter { it.phase.equals("compile") || it.phase.equals("test-compile") }
        val compilerProp = if (executions.isEmpty()) getCompilerProp(body.configuration) else compilerProp(executions)
        if (compilerProp.release == null) {
            compilerProp.release = getLanguageLevel(project.properties.get("maven.compiler.release"))
        }
        if (compilerProp.source == null) {
            compilerProp.source = getLanguageLevel(project.properties.get("maven.compiler.source"))
        }
        if (compilerProp.target == null) {
            compilerProp.target = getLanguageLevel(project.properties.get("maven.compiler.target"))
        }
        if (compilerProp.testRelease == null) {
            compilerProp.testRelease = getLanguageLevel(project.properties.get("maven.compiler.testRelease"))
        }
        if (compilerProp.testSource == null) {
            compilerProp.testSource = getLanguageLevel(project.properties.get("maven.compiler.testSource"))
        }
        if (compilerProp.testTarget == null) {
            compilerProp.testTarget = getLanguageLevel(project.properties.get("maven.compiler.testTarget"))
        }
        return compilerProp;
    }

    private fun compilerProp(executions: List<PluginExecution>) =
        executions.asSequence().map { getCompilerProp(it.configuration) }
            .reduce { acc, next -> sumCompilerProp(acc, next) }

    private fun sumCompilerProp(acc: CompilerProp, next: CompilerProp): CompilerProp {
        acc.release = maxLanguageLevel(acc.release, next.release)
        acc.source = maxLanguageLevel(acc.source, next.source)
        acc.target = maxLanguageLevel(acc.target, next.target)
        acc.testRelease = maxLanguageLevel(acc.testRelease, next.testRelease)
        acc.testSource = maxLanguageLevel(acc.testSource, next.testSource)
        acc.testTarget = maxLanguageLevel(acc.testTarget, next.testTarget)
        return acc;
    }

    private fun maxLanguageLevel(first: LanguageLevel?, second: LanguageLevel?): LanguageLevel? {
        if (first == null) return second
        if (second == null) return first
        return if (first.isLessThan(second)) second else first
    }

    private fun getCompilerProp(configuration: Map<String, Any>): CompilerProp {
        return CompilerProp(
            getLanguageLevel(configuration.get("release")),
            getLanguageLevel(configuration.get("source")),
            getLanguageLevel(configuration.get("target")),
            getLanguageLevel(configuration.get("testRelease")),
            getLanguageLevel(configuration.get("testSource")),
            getLanguageLevel(configuration.get("testTarget")),
        )
    }

    private fun getLanguageLevel(value: Any?): LanguageLevel? {
        return if (value is String) LanguageLevel.parse(value) else null;
    }


    private fun getDefaultCompilerData(plugin: MavenPlugin, localRepositoryPath: Path): CompilerData {
        val descriptor = MavenArtifactUtil.readPluginDescriptor(localRepositoryPath, plugin);
        if (descriptor == null) {
            MavenLog.LOG.warn("null descriptor $plugin")
            return CompilerData(LanguageLevel.HIGHEST, emptyList())
        }
        val source = LanguageLevel.parse(descriptor.myParams.get("source")) ?: LanguageLevel.HIGHEST
        val target = LanguageLevel.parse(descriptor.myParams.get("target")) ?: LanguageLevel.HIGHEST
        return CompilerData(source, target, source, target, emptyList())
    }

    private data class CompilerProp(
        var release: LanguageLevel?, var source: LanguageLevel?, var target: LanguageLevel?,
        var testRelease: LanguageLevel?, var testSource: LanguageLevel?, var testTarget: LanguageLevel?
    )
}