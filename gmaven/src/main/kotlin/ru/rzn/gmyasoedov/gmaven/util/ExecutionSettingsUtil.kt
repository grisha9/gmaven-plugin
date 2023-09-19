@file:JvmName("ExecutionSettingsUtil")

package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionWorkspace
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.ProjectExecution
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils

fun fillExecutionWorkSpace(
    project: Project, projectSettings: MavenProjectSettings, projectPath: String, workspace: MavenExecutionWorkspace
) {
    val projectDataNode = ProjectDataManager.getInstance()
        .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, projectSettings.externalProjectPath)
        ?.externalProjectStructure ?: return

    val profilesStateService = ProjectProfilesStateService.getInstance(project)
    val mainModuleNode = ExternalSystemApiUtil.find(projectDataNode, ProjectKeys.MODULE) ?: return
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
    for (profileDataNode in ExternalSystemApiUtil.findAll<ProfileData>(
        projectDataNode, ProfileData.KEY
    )) {
        val profileExecution = profilesStateService.getProfileExecution(profileDataNode.data)
        if (profileExecution != null) {
            workspace.addProfile(profileExecution)
        }
    }
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

@Suppress("UNCHECKED_CAST")
private fun getParentBuildFile(node: DataNode<ModuleData>): String? {
    val parentGA = node.data.getProperty(GMavenConstants.MODULE_PROP_PARENT_GA)
    return if (parentGA != null && node.parent != null && node.parent!!.data is ModuleData) {
        getParentBuildFile((node.parent as DataNode<ModuleData>?)!!)
    } else node.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
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