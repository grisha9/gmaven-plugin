@file:JvmName("ExecutionSettingsUtil")

package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.exists
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties
import ru.rzn.gmyasoedov.gmaven.settings.*
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.nio.file.Path

fun getDistributionSettings(projectSettings : MavenProjectSettings, project: Project): DistributionSettings {
    val settings = projectSettings.distributionSettings
    if (settings.type != DistributionType.WRAPPER) return settings
    val distributionUrl = MvnDotProperties.getDistributionUrl(project, projectSettings.externalProjectPath)
    if (settings.url == distributionUrl) return settings
    projectSettings.distributionSettings = DistributionSettings.getWrapper(distributionUrl)
    return projectSettings.distributionSettings
}

fun getLocalRepoPath(project: Project, externalProjectPath: String): String? {
    val projectDataNode = ProjectDataManager.getInstance()
        .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, externalProjectPath)
        ?.externalProjectStructure ?: return null
    return ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)
        .map { it.data.getProperty(GMavenConstants.MODULE_PROP_LOCAL_REPO) }
        .firstOrNull()
}

fun fillExecutionWorkSpace(
    project: Project, projectSettings: MavenProjectSettings, projectPath: String, workspace: MavenExecutionWorkspace
) {
    val projectDataNode = ProjectDataManager.getInstance()
        .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, projectSettings.externalProjectPath)
        ?.externalProjectStructure ?: return

    val profilesStateService = ProjectProfilesStateService.getInstance(project)
    val mainModuleNode = ExternalSystemApiUtil.find(projectDataNode, ProjectKeys.MODULE) ?: return
    workspace.externalProjectPath = projectSettings.externalProjectPath
    if (projectSettings.projectBuildFile != null) {
        workspace.projectBuildFile = projectSettings.projectBuildFile
    } else {
        workspace.projectBuildFile = mainModuleNode.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
    }
    val allModules = ExternalSystemApiUtil.findAllRecursively(mainModuleNode, ProjectKeys.MODULE)
    val isRootPath = MavenUtils.equalsPaths(projectSettings.externalProjectPath, projectPath)
    if (!isRootPath) {
        allModules.find { MavenUtils.equalsPaths(it.data.linkedExternalProjectPath, projectPath) }
            ?.let { fillProjectBuildFiles(it, workspace, projectSettings) }
    } else {
        addedIgnoredModule(workspace, allModules)
    }
    for (profileDataNode in ExternalSystemApiUtil.findAll(
        projectDataNode, ProfileData.KEY
    )) {
        val profileExecution = profilesStateService.getProfileExecution(profileDataNode.data)
        if (profileExecution != null) {
            workspace.addProfile(profileExecution)
        }
    }
    setMultiModuleProjectDirectory(projectPath, projectSettings.externalProjectPath, workspace)
}

private fun fillProjectBuildFiles(
    node: DataNode<ModuleData>,
    workspace: MavenExecutionWorkspace,
    projectSettings: MavenProjectSettings
) {
    val module = node.data
    workspace.subProjectBuildFile = module.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
    addedIgnoredModule(workspace, ExternalSystemApiUtil.findAllRecursively(node, ProjectKeys.MODULE))
    if (projectSettings.useWholeProjectContext) {
        val parentBuildFile = getParentBuildFile(node)
        if (parentBuildFile != null) {
            workspace.projectBuildFile = parentBuildFile
            if (MavenUtils.equalsPaths(workspace.projectBuildFile, workspace.subProjectBuildFile)) {
                workspace.subProjectBuildFile = null
            }
        }
        if (workspace.subProjectBuildFile != null) {
            workspace.addProject(ProjectExecution(MavenUtils.toGAString(module), true))
        }
        workspace.subProjectBuildFile = null
    }
}

private fun getParentBuildFile(node: DataNode<ModuleData>): String? {
    val parentGA = node.data.getProperty(GMavenConstants.MODULE_PROP_PARENT_GA)
    val parent = ExternalSystemApiUtil.findParent(node, ProjectKeys.MODULE)
    if (parentGA != null && parent != null) {
        return getParentBuildFile(parent)
    }
    return node.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
}

private fun addedIgnoredModule(
    workspace: MavenExecutionWorkspace,
    allModules: Collection<DataNode<ModuleData>>
) {
    allModules.asSequence()
        .filter { it.isIgnored }
        .map { ProjectExecution(MavenUtils.toGAString(it.data), false) }
        .forEach { data: ProjectExecution? -> workspace.addProject(data) }
}

private fun setMultiModuleProjectDirectory(
    projectPathString: String, externalProjectPath: String?, workspace: MavenExecutionWorkspace
) {
    if (externalProjectPath == null || !Registry.`is`("gmaven.multiModuleProjectDirectory")) return
    val projectPath = Path.of(projectPathString)
    val projectDirPath = if (projectPath.toFile().isDirectory()) projectPath else projectPath.parent
    if (MavenUtils.equalsPaths(projectDirPath.toString(), externalProjectPath)) return
    val mainProjectPath = Path.of(externalProjectPath)
    workspace.multiModuleProjectDirectory = getMultiModuleProjectDirectory(projectDirPath, mainProjectPath).toString()
}

private fun getMultiModuleProjectDirectory(projectPath: Path, mainProjectPath: Path): Path {
    val workingDirectory = if (projectPath.toFile().isDirectory()) projectPath else projectPath.parent
    var projectPathTmp = workingDirectory
    try {
        while (projectPathTmp != mainProjectPath) {
            if (projectPathTmp.resolve(".mvn").exists()) {
                return projectPathTmp
            }
            projectPathTmp = projectPathTmp.parent
        }
        if (projectPathTmp.resolve(".mvn").exists()) {
            return projectPathTmp
        }
    } catch (ignored: Exception) {
    }
    return workingDirectory
}