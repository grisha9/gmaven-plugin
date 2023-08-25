@file:JvmName("ProjectResolverUtils")

package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.MavenRepositoryData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.module.ModuleTypeManager
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.ApacheMavenCompilerPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.CompilerData
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin.KotlinMavenPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin.KotlinMavenPluginData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.*
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MavenWrapperDistribution
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenSettings
import java.nio.file.Path

fun getMavenHome(distributionSettings: DistributionSettings): Path {
    if (distributionSettings.path != null) return distributionSettings.path
    if (distributionSettings.url != null) {
        val mavenHome = MavenWrapperDistribution.getOrDownload(distributionSettings.url)
        distributionSettings.path = mavenHome.path
        return mavenHome.path
    }
    throw ExternalSystemException("maven home is empty")
}

fun getPluginsData(mavenProject: MavenProject, context: MavenProjectResolver.ProjectResolverContext): PluginsData {
    val contentRoots = ArrayList<MavenContentRoot>()
    val excludedRoots = HashSet<String>(4)

    var compilerPlugin: MavenPlugin? = null
    var compilerData: CompilerData? = null
    var kotlinPluginData: KotlinMavenPluginData? = null

    val localRepoPath: String? = context.mavenResult.settings.localRepository
    for (plugin in mavenProject.plugins) {
        val pluginExtension = context.pluginExtensionMap.get(MavenUtils.toGAString(plugin)) ?: continue
        val pluginContentRoot = pluginExtension.getContentRoots(mavenProject, plugin, context)
        contentRoots += pluginContentRoot.contentRoots
        excludedRoots += pluginContentRoot.excludedRoots
        if (pluginExtension is MavenCompilerFullImportPlugin && compilerPlugin == null) {
            compilerPlugin = plugin
            if (localRepoPath != null) {
                compilerData = pluginExtension
                    .getCompilerData(mavenProject, plugin, Path.of(localRepoPath), context.contextElementMap)
            }
        } else if (pluginExtension is KotlinMavenPlugin && kotlinPluginData == null) {
            kotlinPluginData = pluginExtension.getCompilerData(mavenProject, plugin, context)
        }
    }
    return PluginsData(
        compilerPlugin,
        compilerData ?: ApacheMavenCompilerPlugin.getDefaultCompilerData(mavenProject, context.projectLanguageLevel),
        kotlinPluginData,
        PluginContentRoots(contentRoots, excludedRoots)
    )
}

fun getMainJavaCompilerData(
    plugin: MavenPlugin?,
    mavenProject: MavenProject,
    compilerData: CompilerData,
    context: MavenProjectResolver.ProjectResolverContext
): MainJavaCompilerData {
    plugin ?: return MainJavaCompilerData.createDefault()
    val localRepoPath = context.mavenResult.settings.localRepository ?: return MainJavaCompilerData.createDefault()

    return MavenFullImportPlugin.EP_NAME
        .findExtensionOrFail(MavenCompilerFullImportPlugin::class.java)
        .getJavaCompilerData(mavenProject, plugin, compilerData, Path.of(localRepoPath), context.contextElementMap)
}

fun getContentRootPath(paths: List<String>, type: ExternalSystemSourceType): List<MavenContentRoot> {
    return paths.asSequence()
        .filter { it.isNotEmpty() }
        .map { MavenContentRoot(type, it) }
        .toList()
}

fun populateTasks(
    moduleDataNode: DataNode<ModuleData>, mavenProject: MavenProject,
    context: MavenProjectResolver.ProjectResolverContext
) {
    for (basicPhase in GMavenConstants.BASIC_PHASES) {
        moduleDataNode.createChild(LifecycleData.KEY, LifecycleData(SYSTEM_ID, basicPhase, mavenProject.basedir))
    }
    if (!context.settings.isShowPluginNodes) {
        MavenArtifactUtil.clearPluginDescriptorCache()
        return
    }
    val localRepoPath = context.mavenResult.settings.localRepository?.let { Path.of(it) }
    for (plugin in mavenProject.plugins) {
        val pluginDescriptor = MavenArtifactUtil.readPluginDescriptor(localRepoPath, plugin) ?: continue
        for (mojo in pluginDescriptor.mojos) {
            moduleDataNode.createChild(
                PluginData.KEY, PluginData(
                    SYSTEM_ID, mojo.displayName, mavenProject.basedir, plugin.displayString, pluginDescriptor.goalPrefix
                )
            )
        }
    }
}

fun populateRemoteRepository(projectDataNode: DataNode<ProjectData>, mavenSettings: MavenSettings) {
    for (repository in mavenSettings.remoteRepositories) {
        projectDataNode.createChild(
            MavenRepositoryData.KEY, MavenRepositoryData(SYSTEM_ID, repository.id, repository.url)
        )
    }
}

fun populateProfiles(dataNode: DataNode<ProjectData>, mavenSettings: MavenSettings) {
    for (profile in mavenSettings.profiles) {
        dataNode.createChild(
            ProfileData.KEY,
            ProfileData(SYSTEM_ID, dataNode.data.linkedExternalProjectPath, profile.name, profile.isActivation)
        )
    }
}

fun populateAnnotationProcessorData(
    project: MavenProject,
    moduleDataNode: DataNode<ModuleData>,
    compilerData: CompilerData
) {
    val annotationProcessorPaths = compilerData.annotationProcessorPaths
    val data = CompilerPluginData
        .create(annotationProcessorPaths, compilerData.arguments, project.buildDirectory, project.basedir)
    moduleDataNode.createChild(CompilerPluginData.KEY, data)
}

fun getDefaultModuleTypeId(): String {
    return ModuleTypeManager.getInstance().defaultModuleType.id
}

class PluginsData(
    val compilerPlugin: MavenPlugin?, val compilerData: CompilerData,
    val kotlinPluginData: KotlinMavenPluginData?, val contentRoots: PluginContentRoots
)