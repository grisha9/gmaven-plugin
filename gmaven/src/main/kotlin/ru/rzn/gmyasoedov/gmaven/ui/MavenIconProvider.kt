package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import icons.OpenapiIcons.RepositoryLibraryLogo
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleData
import javax.swing.Icon

class MavenIconProvider : DumbAware, FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        project ?: return null
        return try {
            val path = file.toNioPath().toString()
            if (CachedModuleData.getAllConfigPaths(project).contains(path)) RepositoryLibraryLogo else null
        } catch (e: Exception) {
            null
        }
    }
}