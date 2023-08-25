package ru.rzn.gmyasoedov.gmaven.project.importing.kotlin

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.createArguments
import org.jetbrains.kotlin.idea.compiler.configuration.IdeKotlinVersion
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinPluginLayout
import org.jetbrains.kotlin.idea.facet.configureFacet
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.facet.noVersionAutoAdvance
import org.jetbrains.kotlin.idea.facet.parseCompilerArgumentsToFacet
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin.KotlinMavenPluginData

@Order(ExternalSystemConstants.BUILTIN_MODULE_DATA_SERVICE_ORDER + 1)
class KotlinMavenPluginDataService : AbstractProjectDataService<ModuleData, Void>() {

    override fun getTargetDataKey() = ProjectKeys.MODULE

    override fun importData(
        toImport: Collection<DataNode<ModuleData>>,
        projectData: ProjectData?,
        project: Project,
        modifiableModelsProvider: IdeModifiableModelsProvider
    ) {
        for (moduleNode in toImport) {
            val moduleData = moduleNode.data
            val kotlinNode = ExternalSystemApiUtil.find(moduleNode, KotlinMavenPluginData.KEY) ?: continue
            val ideModule = modifiableModelsProvider.findIdeModule(moduleData) ?: continue

            val kotlinData = kotlinNode.data
            val compilerVersion = kotlinData.kotlinVersion.let(IdeKotlinVersion::opt)
                ?: KotlinPluginLayout.instance.standaloneCompilerVersion

            val kotlinFacet = ideModule.getOrCreateFacet(
                modifiableModelsProvider,
                false,
                SYSTEM_ID.id
            )

            val defaultPlatform = JvmIdePlatformKind.defaultPlatform
            kotlinFacet.configureFacet(
                compilerVersion,
                defaultPlatform,
                modifiableModelsProvider
            )

            val sharedArguments = getCompilerArgumentsByConfigurationElement(kotlinData, defaultPlatform)
            parseCompilerArgumentsToFacet(sharedArguments, emptyList(), kotlinFacet, modifiableModelsProvider)

            NoArgMavenProjectImportHandler.invoke(kotlinFacet, kotlinData)
            AllOpenMavenProjectImportHandler.invoke(kotlinFacet, kotlinData)
            kotlinFacet.noVersionAutoAdvance()
        }
    }

    private fun getCompilerArgumentsByConfigurationElement(
        kotlinData: KotlinMavenPluginData,
        platform: TargetPlatform
    ): List<String> {
        val arguments = platform.createArguments()

        arguments.apiVersion = kotlinData.apiVersion
        arguments.languageVersion = kotlinData.languageVersion
        arguments.multiPlatform = false
        arguments.suppressWarnings = kotlinData.noWarn

        when (arguments) {
            is K2JVMCompilerArguments -> {
                arguments.jdkHome = kotlinData.jdkHome
                arguments.jvmTarget = kotlinData.jvmTarget
            }
        }

        parseCommandLineArguments(kotlinData.arguments, arguments)
        return ArgumentUtils.convertArgumentsToStringList(arguments)
    }
}
