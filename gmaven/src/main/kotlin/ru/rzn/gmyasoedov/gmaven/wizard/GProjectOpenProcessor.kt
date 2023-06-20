package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import javax.swing.Icon

internal class GProjectOpenProcessor : ProjectOpenProcessor() {
  private val importProvider = GOpenProjectProvider()

  override fun getName(): String = GMavenConstants.GMAVEN

  override fun getIcon(): Icon = OpenapiIcons.RepositoryLibraryLogo

  override fun canOpenProject(file: VirtualFile): Boolean = importProvider.canOpenProject(file)

  override fun doOpenProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    return importProvider.openProject(projectFile, projectToClose, forceOpenInNewFrame)
  }

  override fun canImportProjectAfterwards(): Boolean = true

  override fun importProjectAfterwards(project: Project, file: VirtualFile) {
    importProvider.linkToExistingProject(file, project)
  }
}