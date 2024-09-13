package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import icons.GMavenIcons
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleDataService
import javax.swing.Icon

class MavenIconProvider : DumbAware, FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        project ?: return null
        val path = file.toNioPathOrNull()?.toString() ?: return null
        val dataHolder = CachedModuleDataService.getDataHolder(project)
        if (dataHolder.activeConfigPaths.contains(path)) return GMavenIcons.MavenProject
        if (dataHolder.ignoredConfigPaths.contains(path)) return GMavenIcons.MavenIgnored
        return null
    }
}