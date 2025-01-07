package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.progress.ModalTaskOwner
import com.intellij.openapi.progress.runBlockingModal
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

internal class GProjectOpenProcessor : ProjectOpenProcessor() {
    private val importProvider = GOpenProjectProvider()

    override fun canOpenProject(file: VirtualFile): Boolean = importProvider.canOpenProject(file)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        return runBlockingModal(ModalTaskOwner.guess(), "") {
            importProvider.openProject(virtualFile, projectToClose, forceOpenInNewFrame)
        }
    }

    override val name = GMavenConstants.SYSTEM_ID.readableName

    override val icon = OpenapiIcons.RepositoryLibraryLogo

    override fun canImportProjectAfterwards(): Boolean = true

    override fun importProjectAfterwards(project: Project, file: VirtualFile) {
        importProvider.linkToExistingProject(file, project)
    }
}