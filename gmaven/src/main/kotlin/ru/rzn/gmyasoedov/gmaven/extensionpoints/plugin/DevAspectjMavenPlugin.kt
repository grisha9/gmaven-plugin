package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.pom.java.LanguageLevel
import org.jdom.Element
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.ApacheMavenCompilerPlugin.Companion.getElement
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.ASPECTJ_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenContentRoot
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.PluginBody
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution
import java.nio.file.Path


const val aspectjToolsArtifactId = "aspectjtools"

class DevAspectjMavenPlugin : MavenCompilerFullImportPlugin {

    override fun getGroupId() = "dev.aspectj"

    override fun getArtifactId() = "aspectj-maven-plugin"

    override fun resolvePlugin() = true

    override fun priority() = 40

    override fun getContentRoots(
        mavenProject: MavenProject,
        plugin: MavenPlugin,
        context: MavenProjectResolver.ProjectResolverContext
    ) = getAspectJContentRoots(mavenProject, plugin, context)

    override fun getCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): CompilerData {
        val compilerProp = getCompilerProp(plugin.body, contextElementMap)
        return toCompilerData(compilerProp, plugin, contextElementMap)
    }

    override fun getJavaCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        compilerData: CompilerData,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): MainJavaCompilerData {
        plugin.body.configuration ?: return MainJavaCompilerData.createDefault()
        val dependenciesPath = getDependencyPathFromDescriptor(plugin, localRepositoryPath)
        val dependencies = dependenciesPath?.let { listOf(it) } ?: emptyList()
        return MainJavaCompilerData.create(ASPECTJ_COMPILER_ID, dependencies, compilerData.arguments)
    }

    private fun toCompilerData(
        compilerProp: CompilerProp,
        plugin: MavenPlugin,
        contextElementMap: MutableMap<String, Element>
    ): CompilerData {
        val configurationElement = plugin.body.configuration?.let { getElement(it, contextElementMap) }
        val args = collectAjcCompilerArgs(configurationElement)
        return CompilerData(
            compilerProp.source, compilerProp.target, compilerProp.source, compilerProp.target,
            emptyList(), args.first, args.second
        )
    }

    private fun collectAjcCompilerArgs(configurationElement: Element?): Pair<List<String>, List<String>> {
        configurationElement ?: return Pair(emptyList(), emptyList())
        val javacArgs = ArrayList<String>()
        val aspectjArgs = ArrayList<String>()

        configurationElement.getChildTextTrim("parameters")
            ?.let { if (it.equals("true", true)) javacArgs.add("-parameters") }
        configurationElement.getChildTextTrim("enablePreview")
            ?.let { if (it.equals("true", true)) javacArgs.add(JavaParameters.JAVA_ENABLE_PREVIEW_PROPERTY) }
        val compilerArgs = configurationElement.getChild("additionalCompilerArgs")
        if (compilerArgs != null) {
            if (compilerArgs.children.isEmpty()) {
                getResolvedText(compilerArgs.value)?.let { javacArgs.add(it) }
            } else {
                for (arg in compilerArgs.children) {
                    getResolvedText(arg)?.let { javacArgs.add(it) }
                }
            }
        }

        addStringParam(configurationElement, aspectjArgs, "Xlint", false)
        addStringParam(configurationElement, aspectjArgs, "Xajruntimetarget", false)
        addStringParam(configurationElement, aspectjArgs, "Xjoinpoints", false)
        addStringParam(configurationElement, aspectjArgs, "proc", false)
        addStringParam(configurationElement, aspectjArgs, "Xlintfile")
        addStringParam(configurationElement, aspectjArgs, "ajdtBuildDefFile")
        addStringParam(configurationElement, aspectjArgs, "argumentFileDirectory")
        addStringParam(configurationElement, aspectjArgs, "argumentFileName")
        addStringParam(configurationElement, aspectjArgs, "bootclasspath")
        addStringParam(configurationElement, aspectjArgs, "encoding")
        addStringParam(configurationElement, aspectjArgs, "outputDirectory")
        addStringParam(configurationElement, aspectjArgs, "outxmlfile")
        addStringParam(configurationElement, aspectjArgs, "repeat")
        addStringParam(configurationElement, aspectjArgs, "xmlConfigured")

        addBooleanParam(configurationElement, aspectjArgs, "XaddSerialVersionUID")
        addBooleanParam(configurationElement, aspectjArgs, "XhasMember")
        addBooleanParam(configurationElement, aspectjArgs, "XnoInline")
        addBooleanParam(configurationElement, aspectjArgs, "XnotReweavable")
        addBooleanParam(configurationElement, aspectjArgs, "Xreweavable")
        addBooleanParam(configurationElement, aspectjArgs, "XserializableAspects")
        addBooleanParam(configurationElement, aspectjArgs, "XterminateAfterCompilation")
        addBooleanParam(configurationElement, aspectjArgs, "crossrefs")
        addBooleanParam(configurationElement, aspectjArgs, "deprecation")
        addBooleanParam(configurationElement, aspectjArgs, "emacssym")
        addBooleanParam(configurationElement, aspectjArgs, "forceAjcCompile")
        addBooleanParam(configurationElement, aspectjArgs, "noImportError")
        addBooleanParam(configurationElement, aspectjArgs, "outxml")
        addBooleanParam(configurationElement, aspectjArgs, "preserveAllLocals")
        addBooleanParam(configurationElement, aspectjArgs, "proceedOnError")
        addBooleanParam(configurationElement, aspectjArgs, "referenceInfo")
        addBooleanParam(configurationElement, aspectjArgs, "showWeaveInfo")
        addBooleanParam(configurationElement, aspectjArgs, "skip")
        addBooleanParam(configurationElement, aspectjArgs, "verbose")
        addBooleanParam(configurationElement, aspectjArgs, "warn")
        return Pair(javacArgs, aspectjArgs)
    }

    private fun getCompilerProp(
        body: PluginBody, contextElementMap: MutableMap<String, Element>
    ): CompilerProp {
        val executions = body.executions
            .filter { it.phase != null && (it.phase.equals("compile") || it.phase.equals("test-compile")) }
        val compilerProp = if (executions.isEmpty())
            getCompilerProp(body.configuration, contextElementMap) else
            compilerProp(executions, contextElementMap)
        return compilerProp
    }

    private fun compilerProp(executions: List<PluginExecution>, contextElementMap: MutableMap<String, Element>) =
        executions.asSequence().map { getCompilerProp(it.configuration, contextElementMap) }
            .reduce { acc, next -> sumCompilerProp(acc, next) }

    private fun sumCompilerProp(acc: CompilerProp, next: CompilerProp): CompilerProp {
        acc.source = maxLanguageLevel(acc.source, next.source)
        acc.target = maxLanguageLevel(acc.target, next.target)
        return acc
    }

    private fun maxLanguageLevel(first: LanguageLevel, second: LanguageLevel): LanguageLevel {
        return if (first.isLessThan(second)) second else first
    }

    private fun getCompilerProp(
        configuration: String?,
        contextElementMap: MutableMap<String, Element>
    ): CompilerProp {
        val element = configuration?.let { getElement(it, contextElementMap) }
            ?: return CompilerProp(LanguageLevel.JDK_1_4, LanguageLevel.JDK_1_4)
        //val compliance = MavenJDOMUtil.findChildValueByPath(element, "complianceLevel")
        var languageLevel = getLanguageLevel(element.getChildTextTrim("release"))
        if (languageLevel != null) return CompilerProp(languageLevel, languageLevel)
        languageLevel = getLanguageLevel(element.getChildTextTrim("complianceLevel"))
        if (languageLevel != null) return CompilerProp(languageLevel, languageLevel)

        val source = getLanguageLevel(element.getChildTextTrim("source")) ?: LanguageLevel.JDK_1_4
        val target = getLanguageLevel(element.getChildTextTrim("target")) ?: LanguageLevel.JDK_1_4
        return CompilerProp(source, target)
    }

    data class CompilerProp(
        var source: LanguageLevel, var target: LanguageLevel,
    )

    companion object {
        fun getDependencyPath(
            plugin: MavenPlugin, localRepositoryPath: Path
        ): String? {
            return (plugin.body.dependencies ?: emptyList()).asSequence()
                .filter { "jar".equals(it.type, true) }
                .filter { it.artifactId == aspectjToolsArtifactId }
                .map {
                    MavenArtifactUtil.getArtifactNioPath(
                        localRepositoryPath, it.groupId, it.artifactId, it.version, it.type
                    ).toString()
                }.firstOrNull()
        }

        fun getDependencyPathFromDescriptor(
            plugin: MavenPlugin, localRepositoryPath: Path
        ): String? {
            val descriptor = MavenArtifactUtil.readPluginDescriptor(localRepositoryPath, plugin, true, false)
            val artifactInfo = descriptor?.findDependency(aspectjToolsArtifactId)
            if (artifactInfo?.v == null) {
                MavenLog.LOG.warn("null descriptor $plugin")
                return null
            }
            return MavenArtifactUtil.getArtifactNioPathJar(
                localRepositoryPath, artifactInfo.g, artifactInfo.a, artifactInfo.v
            ).toString()
        }

        fun addStringParam(
            configurationElement: Element,
            aspectjAgrs: ArrayList<String>,
            paramName: String,
            useSpace: Boolean = true
        ) {
            configurationElement.getChildTextTrim(paramName)
                ?.let { if (it.isNotEmpty()) aspectjAgrs.add(if (useSpace) "-$paramName $it" else "-$paramName:$it") }
        }

        fun addBooleanParam(configurationElement: Element, aspectjAgrs: ArrayList<String>, paramName: String) {
            configurationElement.getChildTextTrim(paramName)
                ?.let { if (it.equals("true", true)) aspectjAgrs.add("-$paramName") }
        }

        fun getAspectJContentRoots(
            mavenProject: MavenProject,
            plugin: MavenPlugin,
            context: MavenProjectResolver.ProjectResolverContext
        ): PluginContentRoots {
            val configuration = getConfiguration(plugin, context)
            val srcPath = MavenJDOMUtil.findChildValueByPath(configuration, "aspectDirectory", "src/main/aspect")
            val testPath = MavenJDOMUtil.findChildValueByPath(configuration, "testAspectDirectory", "src/test/aspect")
            val roots = mutableListOf<MavenContentRoot>()
            if (srcPath.isNotEmpty()) {
                MavenFullImportPlugin.getAbsoluteContentPath(srcPath, mavenProject)
                    ?.let { roots.add(MavenContentRoot(ExternalSystemSourceType.SOURCE, it)) }
            }
            if (testPath.isNotEmpty()) {
                MavenFullImportPlugin.getAbsoluteContentPath(srcPath, mavenProject)
                    ?.let { roots.add(MavenContentRoot(ExternalSystemSourceType.TEST, it)) }
            }
            return PluginContentRoots(roots, emptySet())
        }

        private fun getConfiguration(
            plugin: MavenPlugin,
            context: MavenProjectResolver.ProjectResolverContext
        ): Element? {
            plugin.body ?: return null
            var configuration = plugin.body.configuration?.let { getElement(it, context.contextElementMap) }

            if (configuration == null) {
                configuration = plugin.body.executions.find { it.goals.contains("compile") }
                    ?.configuration
                    ?.let { getElement(it, context.contextElementMap) }
            }
            if (configuration == null) {
                configuration = plugin.body.executions.find { it.goals.contains("test-compile") }
                    ?.configuration
                    ?.let { getElement(it, context.contextElementMap) }
            }
            return configuration
        }
    }
}