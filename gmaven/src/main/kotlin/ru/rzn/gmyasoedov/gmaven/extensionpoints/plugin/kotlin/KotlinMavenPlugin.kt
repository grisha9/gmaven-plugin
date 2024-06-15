package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin

import com.intellij.pom.java.LanguageLevel
import org.jdom.Element
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin.parseConfiguration
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver
import ru.rzn.gmyasoedov.gmaven.util.toFeatureString
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenPlugin
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProject

class KotlinMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.jetbrains.kotlin"

    override fun getArtifactId() = "kotlin-maven-plugin"

    fun getCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        context: MavenProjectResolver.ProjectResolverContext
    ): KotlinMavenPluginData? {
        if (MavenUtils.isPomProject(project)) return null
        val executions = plugin.body?.executions
        executions?.flatMap { it.goals }?.find { it.contains("compile", true) } ?: return null

        val kotlinVersion = plugin.version
        val executionConfigurationElements = executions.map { parseConfiguration(it.configuration, context) }
        val configurationElement = parseConfiguration(plugin.body.configuration, context)
        val jvmTarget = configurationElement.getChild("jvmTarget")?.textTrim
            ?: project.properties["kotlin.compiler.jvmTarget"] as? String
            ?: LanguageLevel.JDK_1_8.toFeatureString()
        val languageVersion = configurationElement.getChild("languageVersion")?.textTrim
            ?: project.properties["kotlin.compiler.languageVersion"] as? String
        val apiVersion = configurationElement.getChild("apiVersion")?.textTrim
            ?: project.properties["kotlin.compiler.apiVersion"] as? String
        val jdkHome = configurationElement.getChild("jdkHome")?.textTrim
            ?: project.properties["kotlin.compiler.jdkHome"] as? String
        val noWarn = configurationElement.getChild("nowarn")?.textTrim == "true"
        val arguments = getArguments(executionConfigurationElements, configurationElement)
        val compilerPlugins = getConfigurationListParams(configurationElement, "compilerPlugins", "plugin")
        val pluginOptions = getConfigurationListParams(configurationElement, "pluginOptions", "option")
        return KotlinMavenPluginData(
            jvmTarget, jdkHome, kotlinVersion, languageVersion, apiVersion,
            noWarn, arguments, compilerPlugins, pluginOptions
        )
    }

    private fun getArguments(executions: List<Element>, configurationElement: Element): List<String> {
        val args = HashSet<String>(getConfigurationListParams(configurationElement, "args", "arg"))
        args += executions.flatMap { getConfigurationListParams(it, "args", "arg") }
        return args.toList()
    }

    private fun getConfigurationListParams(
        configurationElement: Element, rootTag: String, childTag: String
    ): List<String> {
        val result = ArrayList<String>()
        for (element in configurationElement.getChild(rootTag)?.children ?: emptyList()) {
            if (childTag != element.name) continue
            val value = element.textTrim
            if (value != null && value.isNotEmpty()) {
                result.add(value)
            }
        }
        return result
    }
}