package ru.rzn.gmyasoedov.gmaven.autolink

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.gmaven.wizard.GProjectOpenProcessor
import java.nio.file.Path

class UnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
    override val systemId = SYSTEM_ID

    override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean {
        return MavenUtils.isPomFileName(buildFile.name) || MavenUtils.isPotentialPomFile(buildFile.name)
    }

    override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean {
        return MavenSettings.getInstance(project).getLinkedProjectSettings(externalProjectPath) != null
    }

    override fun subscribe(
        project: Project,
        listener: ExternalSystemProjectLinkListener,
        parentDisposable: Disposable
    ) {

    }

    override fun linkAndLoadProject(project: Project, externalProjectPath: String) {
        ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(GProjectOpenProcessor::class.java)
            .importProjectAfterwards(project, MavenUtils.getVFile(Path.of(externalProjectPath).toFile()))
    }
}