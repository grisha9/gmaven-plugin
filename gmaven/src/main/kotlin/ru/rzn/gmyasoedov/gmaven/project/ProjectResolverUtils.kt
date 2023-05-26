package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.MavenRepositoryData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.pom.java.LanguageLevel
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.CompilerData
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.LifecycleData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.ActivationProfile.INDETERMINATE
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.SimpleProfile.INACTIVE
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MavenWrapperDistribution
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.serverapi.model.MavenProfile
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import ru.rzn.gmyasoedov.serverapi.model.MavenSettings
import java.nio.file.Path
import java.util.*

fun getMavenHome(distributionSettings: DistributionSettings): Path {
    if (distributionSettings.path != null) return distributionSettings.path
    if (distributionSettings.url != null) {
        val mavenHome = MavenWrapperDistribution.getOrDownload(distributionSettings.url)
        return mavenHome.path
    }
    throw ExternalSystemException("maven home is empty")
}

fun getCompilerData(mavenProject: MavenProject, mavenResult: MavenResult, projectLanguageLevel: LanguageLevel):
        CompilerData {
    val localRepoPath = mavenResult.settings.localRepository
        ?: return CompilerData(projectLanguageLevel, Collections.emptyList())
    val compilerPlugin = MavenFullImportPlugin.EP_NAME.extensionList
        .filterIsInstance<MavenCompilerFullImportPlugin>()
        .firstOrNull() ?: return CompilerData(projectLanguageLevel, Collections.emptyList())

    for (plugin in mavenProject.plugins) {
        if (compilerPlugin.isApplicable(plugin)) {
            return compilerPlugin.getCompilerData(mavenProject, plugin, Path.of(localRepoPath))
        }
    }
    return CompilerData(projectLanguageLevel, Collections.emptyList())
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

fun populateProfiles(dataNode: DataNode<ModuleData>, mavenSettings: MavenSettings, settings: MavenExecutionSettings) {
    for (profile in mavenSettings.profiles) {
        dataNode.createChild(ProfileData.KEY, getProfileData(profile, settings))
    }
}

private fun getProfileData(profile: MavenProfile, settings: MavenExecutionSettings): ProfileData {
    val profileData = settings.executionWorkspace.profilesData
        .firstOrNull { it.name == profile.name && it.hasActivation() == profile.isActivation }
    if (profileData != null) {
        return profileData
    }
    return if (profile.isActivation) ProfileData(SYSTEM_ID, profile.name, null, INDETERMINATE)
    else ProfileData(SYSTEM_ID, profile.name, INACTIVE, null)
}