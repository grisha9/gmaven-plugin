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
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.CompilerData
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.*
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MavenWrapperDistribution
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
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

fun getCompilerData(mavenProject: MavenProject, context: MavenProjectResolver.ProjectResolverContext):
        CompilerData {
    val projectLanguageLevel = context.projectLanguageLevel
    val localRepoPath = context.mavenResult.settings.localRepository
        ?: return CompilerData(projectLanguageLevel, emptyList(), emptyList())
    val compilerPlugin = MavenFullImportPlugin.EP_NAME.extensionList
        .filterIsInstance<MavenCompilerFullImportPlugin>()
        .firstOrNull() ?: return CompilerData(projectLanguageLevel, emptyList(), emptyList())

    for (plugin in mavenProject.plugins) {
        if (compilerPlugin.isApplicable(plugin)) {
            return compilerPlugin
                .getCompilerData(mavenProject, plugin, Path.of(localRepoPath), context.contextElementMap)
        }
    }
    return CompilerData(projectLanguageLevel, emptyList(), emptyList())
}

fun getContentRootPath(paths: List<String>, type: ExternalSystemSourceType): List<MavenContentRoot> {
    return paths.asSequence()
        .filter { it.isNotEmpty() }
        .map { MavenContentRoot(type, it) }
        .toList()
}

fun getPluginContentRootPaths(mavenProject: MavenProject): PluginContentRoots {
    val contentRoots = ArrayList<MavenContentRoot>()
    val excludedRoots = HashSet<String>(4)
    for (plugin in mavenProject.plugins) {
        for (pluginExtension in MavenFullImportPlugin.EP_NAME.extensionList) {
            if (pluginExtension.isApplicable(plugin)) {
                val pluginContentRoot = pluginExtension.getContentRoots(mavenProject, plugin)
                contentRoots += pluginContentRoot.contentRoots
                excludedRoots += pluginContentRoot.excludedRoots
            }
        }
    }
    return PluginContentRoots(contentRoots, excludedRoots);
}

fun populateTasks(moduleDataNode: DataNode<ModuleData>, mavenProject: MavenProject, localRepo: Path?) {
    for (basicPhase in GMavenConstants.BASIC_PHASES) {
        moduleDataNode.createChild(LifecycleData.KEY, LifecycleData(SYSTEM_ID, basicPhase, mavenProject.basedir))
    }
    for (plugin in mavenProject.plugins) {
        val pluginDescriptor = MavenArtifactUtil.readPluginDescriptor(localRepo, plugin) ?: continue
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