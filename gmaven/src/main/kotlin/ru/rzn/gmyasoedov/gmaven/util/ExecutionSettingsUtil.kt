@file:JvmName("ExecutionSettingsUtil")

package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties
import ru.rzn.gmyasoedov.gmaven.settings.*
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

fun getDistributionSettings(projectSettings: MavenProjectSettings, project: Project): DistributionSettings {
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

    val allModules = ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)
    val mainModuleNode = allModules.first { it.data.linkedExternalProjectPath == projectSettings.externalProjectPath }
    workspace.externalProjectPath = projectSettings.externalProjectPath
    workspace.projectBuildFile = if (projectSettings.projectBuildFile != null) projectSettings.projectBuildFile else
        mainModuleNode.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)

    val isRootPath = MavenUtils.equalsPaths(projectSettings.externalProjectPath, projectPath)
    var targetModuleNode: DataNode<ModuleData>? = null
    if (!isRootPath) {
        val contextPair = getTargetModuleAndContextMap(projectPath, allModules)
        targetModuleNode = contextPair.first
        targetModuleNode?.let { fillProjectBuildFiles(it, workspace, projectSettings, contextPair.second) }
    }
    addedIgnoredModule(workspace, allModules, targetModuleNode)
    addedProfiles(projectDataNode, ProjectProfilesStateService.getInstance(project), workspace)
    setMultiModuleProjectDirectory(projectSettings.externalProjectPath, workspace)
}

private fun getTargetModuleAndContextMap(
    projectPath: String, allModules: Collection<DataNode<ModuleData>>
): Pair<DataNode<ModuleData>?, TreeMap<String, DataNode<ModuleData>>> {
    val moduleByInternalName = TreeMap<String, DataNode<ModuleData>>()
    var targetModuleNode: DataNode<ModuleData>? = null
    for (each in allModules) {
        moduleByInternalName[each.data.internalName] = each
        if (targetModuleNode == null && MavenUtils.equalsPaths(each.data.linkedExternalProjectPath, projectPath)) {
            targetModuleNode = each
        }
    }
    return targetModuleNode to moduleByInternalName
}

private fun fillProjectBuildFiles(
    node: DataNode<ModuleData>,
    workspace: MavenExecutionWorkspace,
    projectSettings: MavenProjectSettings,
    moduleByInternalName: TreeMap<String, DataNode<ModuleData>>
) {
    val module = node.data
    workspace.subProjectBuildFile = module.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
    if (projectSettings.useWholeProjectContext) {
        val parentBuildFile = getParentBuildFile(node, moduleByInternalName)
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

private fun getParentBuildFile(
    node: DataNode<ModuleData>, moduleByInternalName: TreeMap<String, DataNode<ModuleData>>
): String? {
    val parentGA = node.data.getProperty(GMavenConstants.MODULE_PROP_PARENT_GA)
    if (parentGA != null) {
        val parentModule = node.data.ideParentGrouping?.let { moduleByInternalName[it] }
        if (parentModule != null) {
            return getParentBuildFile(parentModule, moduleByInternalName)
        }
    }
    return node.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
}

private fun addedIgnoredModule(
    workspace: MavenExecutionWorkspace,
    allModules: Collection<DataNode<ModuleData>>,
    targetModuleNode: DataNode<ModuleData>?
) {
    val basePrefixInternalName = if (targetModuleNode != null) {
        targetModuleNode.data.internalName
    } else {
        val buildFile = workspace.subProjectBuildFile ?: workspace.projectBuildFile ?: return
        val parentNode = allModules
            .find { it.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) == buildFile } ?: return
        parentNode.data.internalName
    } + "."
    allModules.asSequence()
        .filter { it.isIgnored }
        .filter { it.data.internalName.startsWith(basePrefixInternalName) }
        .map { ProjectExecution(MavenUtils.toGAString(it.data), false) }
        .forEach { data: ProjectExecution? -> workspace.addProject(data) }
}

private fun addedProfiles(
    projectDataNode: DataNode<ProjectData>,
    profilesStateService: ProjectProfilesStateService,
    workspace: MavenExecutionWorkspace
) {
    for (profileDataNode in ExternalSystemApiUtil.findAll(projectDataNode, ProfileData.KEY)) {
        profilesStateService.getProfileExecution(profileDataNode.data)?.let { workspace.addProfile(it) }
    }
}

private fun setMultiModuleProjectDirectory(
    externalProjectPath: String?, workspace: MavenExecutionWorkspace
) {
    if (externalProjectPath == null || !Registry.`is`("gmaven.multiModuleProjectDirectory")) return
    val projectPathString = workspace.subProjectBuildFile ?: workspace.projectBuildFile ?: return
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