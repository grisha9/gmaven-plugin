package ru.rzn.gmyasoedov.gmaven.project.importing.kotlin

import org.jetbrains.kotlin.idea.compilerPlugin.AnnotationBasedCompilerPluginSetup
import org.jetbrains.kotlin.idea.compilerPlugin.modifyCompilerArgumentsForPlugin
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin.KotlinMavenPluginData
import java.io.File

abstract class AbstractMavenImportHandler {
    abstract val compilerPluginId: String
    abstract val pluginName: String
    abstract val pluginJarFileFromIdea: File

    fun invoke(facet: KotlinFacet, kotlinMavenPluginData: KotlinMavenPluginData) {
        modifyCompilerArgumentsForPlugin(facet, getPluginSetup(kotlinMavenPluginData),
            compilerPluginId = compilerPluginId,
            pluginName = pluginName)
    }

    abstract fun getOptions(
        enabledCompilerPlugins: List<String>,
        compilerPluginOptions: List<String>
    ): List<AnnotationBasedCompilerPluginSetup.PluginOption>?

    private fun getPluginSetup(kotlinMavenPluginData: KotlinMavenPluginData): AnnotationBasedCompilerPluginSetup? {
        val enabledCompilerPlugins = kotlinMavenPluginData.compilerPlugins
        val compilerPluginOptions = kotlinMavenPluginData.pluginOptions
        val classpath = listOf(pluginJarFileFromIdea.absolutePath)
        val options = getOptions(enabledCompilerPlugins, compilerPluginOptions) ?: return null
        return AnnotationBasedCompilerPluginSetup(options, classpath)
    }
}