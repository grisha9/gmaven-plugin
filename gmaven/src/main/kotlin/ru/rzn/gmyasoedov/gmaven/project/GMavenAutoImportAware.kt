package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.CompilerProjectExtension
import com.intellij.openapi.vfs.VfsUtilCore
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory

class GMavenAutoImportAware : ExternalSystemAutoImportAware {

    override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? {
        val changedPath = Path.of(changedFileOrDirPath)
        if (changedPath.isDirectory()) return null
        val fileSimpleName = changedPath.fileName.toString()
        if (!MavenUtils.isPomFileName(fileSimpleName) && !MavenUtils.isPotentialPomFile(fileSimpleName)) return null
        if (isInsideCompileOutput(changedFileOrDirPath, project)) return null

        val systemSettings = MavenSettings.getInstance(project)
        val projectsSettings = systemSettings.linkedProjectsSettings
        if (projectsSettings.isEmpty()) {
            return null
        }

        return systemSettings.getLinkedProjectSettings(changedPath.parent.toString())?.externalProjectPath
    }

    override fun getAffectedExternalProjectFiles(projectPath: String?, project: Project): MutableList<File> {
        projectPath ?: return mutableListOf()
        val systemSettings = MavenSettings.getInstance(project)
        val projectSettings = systemSettings.getLinkedProjectSettings(projectPath) ?: return mutableListOf()
        val projectData = ProjectDataManager.getInstance()
            .getExternalProjectData(project, SYSTEM_ID, projectSettings.externalProjectPath)
        val parentDataNode = projectData?.externalProjectStructure ?: return mutableListOf()
        return ExternalSystemApiUtil.findAll(parentDataNode, ProjectKeys.MODULE)
            .asSequence()
            .map { it.data }
            .filter { it.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) is String }
            .map { File(it.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) as String) }
            .toMutableList()
    }

    override fun isApplicable(resolverPolicy: ProjectResolverPolicy?): Boolean {
        return true
    }

    private fun isInsideCompileOutput(path: String, project: Project): Boolean {
        val url = VfsUtilCore.pathToUrl(path)
        val compilerOutputUrl = CompilerProjectExtension.getInstance(project)?.compilerOutputUrl ?: return false
        val isInsideProjectCompile = VfsUtilCore.isEqualOrAncestor(compilerOutputUrl, url)
        if (isInsideProjectCompile) return true

        return ModuleManager.getInstance(project).modules
            .asSequence()
            .map { CompilerModuleExtension.getInstance(it) }
            .filterNotNull()
            .flatMap { listOf(it.compilerOutputUrl, it.compilerOutputUrlForTests) }
            .filterNotNull()
            .any { VfsUtilCore.isEqualOrAncestor(it, url) }
    }
}