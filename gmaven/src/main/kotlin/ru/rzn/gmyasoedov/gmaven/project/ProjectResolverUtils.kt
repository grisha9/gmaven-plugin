@file:JvmName("ProjectResolverUtils")

package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.MavenRepositoryData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.module.ModuleTypeManager
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.*
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin.KotlinMavenPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin.KotlinMavenPluginData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.*
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.ASPECTJ_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.notification.OpenGMavenSettingsCallback
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MavenWrapperDistribution
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenRemoteRepository
import ru.rzn.gmyasoedov.serverapi.model.MavenSettings
import java.nio.file.Path
import kotlin.io.path.Path

fun getMavenHome(distributionSettings: DistributionSettings): Path {
    if (distributionSettings.path != null) return distributionSettings.path
    if (distributionSettings.url != null) {
        val mavenHome = MavenWrapperDistribution.getOrDownload(distributionSettings.url)
        distributionSettings.path = mavenHome.path
        return mavenHome.path
    }
    val quickFixId = OpenGMavenSettingsCallback.ID
    throw ExternalSystemException(GBundle.message("gmaven.notification.mvn.not.found", quickFixId), quickFixId)
}

fun getPluginsData(mavenProject: MavenProject, context: MavenProjectResolver.ProjectResolverContext): PluginsData {
    val contentRoots = ArrayList<MavenContentRoot>()
    val excludedRoots = HashSet<String>(4)
    val annotationProcessorPaths = ArrayList<String>(1)

    var compilerPlugin: MavenPlugin? = null
    var compilerData: CompilerData? = null
    var kotlinPluginData: KotlinMavenPluginData? = null

    val localRepoPath: String? = context.mavenResult.settings.localRepository
    for (plugin in mavenProject.plugins) {
        val pluginExtension = context.pluginExtensionMap[MavenUtils.toGAString(plugin)] ?: continue
        val pluginContentRoot = pluginExtension.getContentRoots(mavenProject, plugin, context)
        contentRoots += pluginContentRoot.contentRoots
        excludedRoots += pluginContentRoot.excludedRoots
        if (pluginExtension is MavenCompilerFullImportPlugin) {
            val compilerDataPlugin = getCompilerData(localRepoPath, pluginExtension, mavenProject, plugin, context)
            annotationProcessorPaths += compilerDataPlugin?.annotationProcessorPaths ?: emptyList()
            if (isPriorityCompiler(compilerPlugin, plugin, context)) {
                compilerPlugin = plugin
                compilerData = compilerDataPlugin
            }
        } else if (pluginExtension is KotlinMavenPlugin && kotlinPluginData == null) {
            kotlinPluginData = pluginExtension.getCompilerData(mavenProject, plugin, context)
        }
    }
    compilerData = applyAnnotationProcessorsPath(compilerData, annotationProcessorPaths)
    addedMavenAspectJPluginInfo(compilerPlugin, compilerData, context)
    return PluginsData(
        compilerPlugin,
        compilerData ?: ApacheMavenCompilerPlugin.getDefaultCompilerData(mavenProject, context.projectLanguageLevel),
        kotlinPluginData,
        PluginContentRoots(contentRoots, excludedRoots)
    )
}

private fun getCompilerData(
    localRepoPath: String?,
    pluginExtension: MavenCompilerFullImportPlugin,
    mavenProject: MavenProject,
    plugin: MavenPlugin,
    context: MavenProjectResolver.ProjectResolverContext
): CompilerData? {
    localRepoPath ?: return null
    return pluginExtension.getCompilerData(mavenProject, plugin, Path.of(localRepoPath), context.contextElementMap)
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
    for (basicPhase in context.lifecycles) {
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

fun populateRemoteRepository(projectDataNode: DataNode<ProjectData>, remoteRepositories: Set<MavenRemoteRepository>) {
    for (repository in remoteRepositories) {
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

private fun applyAnnotationProcessorsPath(
    compilerData: CompilerData?,  annotationProcessorPaths: List<String>
): CompilerData? {
    compilerData ?: return null
    if (annotationProcessorPaths.isEmpty()) return compilerData
    if (compilerData.annotationProcessorPaths == annotationProcessorPaths) return compilerData
    return CompilerData(
        compilerData.sourceLevel, compilerData.targetLevel,
        compilerData.testSourceLevel, compilerData.testTargetLevel,
        annotationProcessorPaths, compilerData.arguments, compilerData.pluginSpecificArguments
    )
}

private fun addedMavenAspectJPluginInfo(
    plugin: MavenPlugin?,
    compilerData: CompilerData?,
    context: MavenProjectResolver.ProjectResolverContext,
) {
    if (plugin?.artifactId == "aspectj-maven-plugin" && compilerData != null) {
        context.aspectJCompilerData.add(MavenProjectResolver.CompilerDataHolder(plugin, compilerData))
    }
}

fun getAjcCompilerData(context: MavenProjectResolver.ProjectResolverContext): MainJavaCompilerData? {
    val aspectJCompilerData = context.aspectJCompilerData
    if (aspectJCompilerData.isNotEmpty()) {
        val localRepository = context.mavenResult.settings.localRepository?.let { Path(it) } ?: return null
        var dependenciesPath = aspectJCompilerData
            .firstNotNullOfOrNull { DevAspectjMavenPlugin.getDependencyPath(it.plugin, localRepository) }
        if (dependenciesPath == null) {
            dependenciesPath = aspectJCompilerData
                .firstNotNullOfOrNull {
                    DevAspectjMavenPlugin.getDependencyPathFromDescriptor(it.plugin, localRepository)
                }
        }
        dependenciesPath ?: return null
        val arguments = aspectJCompilerData.flatMapTo(mutableSetOf()) { it.compilerData.pluginSpecificArguments }
        return MainJavaCompilerData.create(ASPECTJ_COMPILER_ID, listOf(dependenciesPath), arguments)
    }
    return null
}

private fun isPriorityCompiler(
    currentPlugin: MavenPlugin?, plugin: MavenPlugin, context: MavenProjectResolver.ProjectResolverContext
): Boolean {
    currentPlugin ?: return true
    val currentPriority = (context.pluginExtensionMap[MavenUtils.toGAString(currentPlugin)]
            as? MavenCompilerFullImportPlugin)?.priority() ?: 0
    val priority = (context.pluginExtensionMap[MavenUtils.toGAString(plugin)]
            as? MavenCompilerFullImportPlugin)?.priority() ?: 0
    return priority > currentPriority
}

class PluginsData(
    val compilerPlugin: MavenPlugin?, val compilerData: CompilerData,
    val kotlinPluginData: KotlinMavenPluginData?, val contentRoots: PluginContentRoots
)