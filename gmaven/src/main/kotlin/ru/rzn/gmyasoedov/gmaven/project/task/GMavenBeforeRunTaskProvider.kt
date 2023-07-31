package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID

class GMavenBeforeRunTaskProvider(project: Project) : ExternalSystemBeforeRunTaskProvider(SYSTEM_ID, project, ID),
    DumbAware {

    override fun getIcon() = OpenapiIcons.RepositoryLibraryLogo

    override fun getTaskIcon(task: ExternalSystemBeforeRunTask?) = OpenapiIcons.RepositoryLibraryLogo

    override fun createTask(runConfiguration: RunConfiguration) = ExternalSystemBeforeRunTask(ID, SYSTEM_ID)

    companion object {
        val ID = Key.create<ExternalSystemBeforeRunTask>("${SYSTEM_ID.readableName}.BeforeRunTask")
    }
}