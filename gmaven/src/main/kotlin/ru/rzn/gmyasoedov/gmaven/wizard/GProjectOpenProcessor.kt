package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.projectImport.ProjectOpenProcessor
import icons.GMavenIcons
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

internal class GProjectOpenProcessor : ProjectOpenProcessor() {
    private val importProvider = GOpenProjectProvider()

    override fun canOpenProject(file: VirtualFile): Boolean = importProvider.canOpenProject(file)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        return runWithModalProgressBlocking(ModalTaskOwner.guess(), "") {
            importProvider.openProject(virtualFile, projectToClose, forceOpenInNewFrame)
        }
    }

    override val name = GMavenConstants.SYSTEM_ID.readableName

    override val icon = GMavenIcons.MavenProject

    override fun canImportProjectAfterwards(): Boolean = true

    override fun importProjectAfterwards(project: Project, file: VirtualFile) {
        importProvider.linkToExistingProject(file, project)
    }
}