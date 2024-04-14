package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.pom.java.LanguageLevel
import org.jdom.Element
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.ApacheMavenCompilerPlugin.Companion.getElement
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.PluginBody
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution
import java.nio.file.Path

class CodehausAspectjMavenPlugin : MavenCompilerFullImportPlugin {

    override fun getGroupId() = "org.codehaus.mojo"

    override fun getArtifactId() = "aspectj-maven-plugin"

    override fun resolvePlugin() = true

    override fun priority() = 30

    override fun getContentRoots(
        mavenProject: MavenProject,
        plugin: MavenPlugin,
        context: MavenProjectResolver.ProjectResolverContext
    ) = DevAspectjMavenPlugin.getAspectJContentRoots(mavenProject, plugin, context)

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
        return DevAspectjMavenPlugin()
            .getJavaCompilerData(project, plugin, compilerData, localRepositoryPath, contextElementMap)
    }

    private fun toCompilerData(
        compilerProp: CompilerProp,
        plugin: MavenPlugin,
        contextElementMap: MutableMap<String, Element>
    ): CompilerData {
        val configurationElement = plugin.body.configuration?.let { getElement(it, contextElementMap) }
        val aspectjArgs = collectAjcCompilerArgs(configurationElement)
        return CompilerData(
            compilerProp.source, compilerProp.target, compilerProp.source, compilerProp.target,
            plugin.body.annotationProcessorPaths, emptyList(), aspectjArgs
        )
    }

    private fun collectAjcCompilerArgs(configurationElement: Element?): Collection<String> {
        configurationElement ?: return emptyList()
        val aspectjArgs = ArrayList<String>()

        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "Xlint", false)
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "ajdtBuildDefFile")
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "argumentFileName")
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "bootclasspath")
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "encoding")
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "outputDirectory")
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "outxmlfile")
        DevAspectjMavenPlugin.addStringParam(configurationElement, aspectjArgs, "repeat")

        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "XaddSerialVersionUID")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "XhasMember")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "XnoInline")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "Xreweavable")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "XserializableAspects")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "XterminateAfterCompilation")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "deprecation")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "emacssym")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "forceAjcCompile")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "noImportError")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "outxml")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "preserveAllLocals")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "proceedOnError")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "referenceInfo")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "showWeaveInfo")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "skip")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "verbose")
        DevAspectjMavenPlugin.addBooleanParam(configurationElement, aspectjArgs, "warn")

        return aspectjArgs
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

    private fun getCompilerProp(configuration: String?, contextElementMap: MutableMap<String, Element>): CompilerProp {
        val element = configuration?.let { getElement(it, contextElementMap) }
            ?: return CompilerProp(LanguageLevel.JDK_1_4, LanguageLevel.JDK_1_4)
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
}