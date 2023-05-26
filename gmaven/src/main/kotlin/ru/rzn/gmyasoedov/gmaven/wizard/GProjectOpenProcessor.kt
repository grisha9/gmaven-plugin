package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import javax.swing.Icon

internal class GProjectOpenProcessor : ProjectOpenProcessor() {
  private val importProvider = GOpenProjectProvider()

  override fun getName(): String = importProvider.builder.name

  override fun getIcon(): Icon = importProvider.builder.icon

  override fun canOpenProject(file: VirtualFile): Boolean = importProvider.canOpenProject(file)

  override fun doOpenProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    return importProvider.openProject(projectFile, projectToClose, forceOpenInNewFrame)
  }

  override fun canImportProjectAfterwards(): Boolean = true

  override fun importProjectAfterwards(project: Project, file: VirtualFile) {
    importProvider.linkToExistingProject(file, project)
  }
}