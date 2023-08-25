package ru.rzn.gmyasoedov.gmaven.project.importing.kotlin

import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.compilerPlugin.CompilerPluginSetup
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor

object NoArgMavenProjectImportHandler : AbstractMavenImportHandler() {

    private const val ANNOTATATION_PARAMETER_PREFIX = "no-arg:annotation="
    private const val INVOKEINITIALIZERS_PARAMETER_PREFIX = "no-arg:invokeInitializers="
    private val SUPPORTED_PRESETS = mapOf(
        "jpa" to listOf(
            "javax.persistence.Entity",
            "javax.persistence.Embeddable",
            "javax.persistence.MappedSuperclass"
        )
    )

    override val compilerPluginId = "org.jetbrains.kotlin.noarg"
    override val pluginName = "noarg"
    override val pluginJarFileFromIdea = KotlinArtifacts.instance.noargCompilerPlugin

    override fun getOptions(
        enabledCompilerPlugins: List<String>,
        compilerPluginOptions: List<String>
    ): List<CompilerPluginSetup.PluginOption>? {
        if ("no-arg" !in enabledCompilerPlugins && "jpa" !in enabledCompilerPlugins) {
            return null
        }

        val annotations = mutableListOf<String>()
        for ((presetName, presetAnnotations) in SUPPORTED_PRESETS) {
            if (presetName in enabledCompilerPlugins) {
                annotations.addAll(presetAnnotations)
            }
        }

        annotations.addAll(compilerPluginOptions.mapNotNull { text ->
            if (!text.startsWith(ANNOTATATION_PARAMETER_PREFIX)) return@mapNotNull null
            text.substring(ANNOTATATION_PARAMETER_PREFIX.length)
        })

        val options = annotations.mapTo(mutableListOf()) {
            CompilerPluginSetup.PluginOption(
                NoArgCommandLineProcessor.ANNOTATION_OPTION.optionName,
                it
            )
        }

        val invokeInitializerOptionValue = compilerPluginOptions
            .firstOrNull { it.startsWith(INVOKEINITIALIZERS_PARAMETER_PREFIX) }
            ?.drop(INVOKEINITIALIZERS_PARAMETER_PREFIX.length) == "true"

        if (invokeInitializerOptionValue) {
            options.add(
                CompilerPluginSetup.PluginOption(
                    NoArgCommandLineProcessor.INVOKE_INITIALIZERS_OPTION.optionName,
                    "true"
                )
            )
        }

        return options
    }
}
