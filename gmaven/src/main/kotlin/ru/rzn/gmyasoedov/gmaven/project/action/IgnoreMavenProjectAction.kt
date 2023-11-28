package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.action.ExternalSystemToggleAction
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleData
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import java.nio.file.Path
import kotlin.io.path.isDirectory

class IgnoreMavenProjectAction : ExternalSystemToggleAction() {

    init {
        setText(GBundle.message("gmaven.action.IgnoreExternalProject.text"))
        setDescription(GBundle.message("gmaven.action.IgnoreExternalProject.description"))
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun isVisible(e: AnActionEvent): Boolean {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return false
        val project = e.getData(CommonDataKeys.PROJECT) ?: return false
        val nioPath = virtualFile.toNioPathOrNull()?.toString() ?: return false

        return if (virtualFile.isDirectory)
            MavenSettings.getInstance(project).getLinkedProjectSettings(nioPath) != null
        else
            CachedModuleData.getAllConfigPaths(project).contains(nioPath)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val nioPath = e.getData(CommonDataKeys.VIRTUAL_FILE)?.toNioPathOrNull() ?: return
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        val projectDataNode = getProjectDataNode(project, nioPath) ?: return
        val allModuleNodes = ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)
        val currentModuleNode = allModuleNodes.find { isSelectedModuleNode(it, nioPath) } ?: return
        val basePrefixName = currentModuleNode.data.internalName + "."
        ExternalProjectsManager.getInstance(project).setIgnored(currentModuleNode, state)
        try {
            allModuleNodes
                .filter { it.data.internalName.startsWith(basePrefixName) }
                .forEach { ExternalProjectsManager.getInstance(project).setIgnored(it, state) }

            // async import to not block UI on big projects
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, e.presentation.text, false) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        ApplicationManager.getApplication()
                            .getService(ProjectDataManager::class.java).importData(projectDataNode, project)
                    } catch (ignored: Exception) {
                    }
                }
            })
        } catch (e: Exception) {
            MavenLog.LOG.warn(e)
        }
    }

    override fun doIsSelected(e: AnActionEvent): Boolean {
        try {
            val nioPath = e.getData(CommonDataKeys.VIRTUAL_FILE)?.toNioPathOrNull() ?: return false
            val project = e.getData(CommonDataKeys.PROJECT) ?: return false
            val projectDataNode = getProjectDataNode(project, nioPath) ?: return false
            val allModuleNodes = ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)
            return allModuleNodes.find { isSelectedModuleNode(it, nioPath) }?.isIgnored ?: return false
        } catch (e: Exception) {
            MavenLog.LOG.warn(e)
            return false
        }
    }

    private fun isSelectedModuleNode(moduleNode: DataNode<ModuleData>, path: Path): Boolean {
        return if (path.isDirectory())
            moduleNode.data.linkedExternalProjectPath == path.toString()
        else
            moduleNode.data.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) == path.toString()
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val selected = super.isSelected(e)
        if (selected) {
            setText(e, GBundle.message("gmaven.action.UnignoreExternalProject.text"));
        } else {
            setText(e, GBundle.message("gmaven.action.IgnoreExternalProject.text"));
        }
        return selected
    }

    private fun getProjectDataNode(project: Project, selectedFilePath: Path): DataNode<ProjectData>? {
        val linkedProjectPath = if (selectedFilePath.isDirectory()) selectedFilePath.toString() else
            selectedFilePath.parent.toString()
        val projectSettings = MavenSettings.getInstance(project)
            .getLinkedProjectSettings(linkedProjectPath) ?: return null
        return ProjectDataManager.getInstance()
            .getExternalProjectData(project, SYSTEM_ID, projectSettings.externalProjectPath)
            ?.externalProjectStructure
    }
}