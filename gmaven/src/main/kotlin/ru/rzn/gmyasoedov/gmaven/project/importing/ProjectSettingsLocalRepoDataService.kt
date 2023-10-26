package ru.rzn.gmyasoedov.gmaven.project.importing

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.roots.findAll
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.MODULE_PROP_LOCAL_REPO
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings

@Order(ExternalSystemConstants.UNORDERED)
class ProjectSettingsLocalRepoDataService : AbstractProjectDataService<ProjectData, Void>() {

    override fun getTargetDataKey() = ProjectKeys.PROJECT

    override fun importData(
        projectNodesToImport: MutableCollection<out DataNode<ProjectData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        for (dataNode in projectNodesToImport) {
            val projectPath = dataNode.data.linkedExternalProjectPath
            val projectSettings = MavenSettings.getInstance(project).getLinkedProjectSettings(projectPath) ?: continue
            dataNode.findAll(ProjectKeys.MODULE)
                .map { it.data.getProperty(MODULE_PROP_LOCAL_REPO) }
                .firstOrNull()
                ?.also { projectSettings.localRepositoryPath = it }
        }
    }
}