package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.configuration.*
import com.intellij.openapi.externalSystem.service.ui.project.path.ExternalSystemWorkingDirectoryInfo
import com.intellij.openapi.externalSystem.service.ui.project.path.WorkingDirectoryField
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.execution.ParametersListUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.execution.GMavenRunConfiguration
import java.util.*

class GMavenRunConfigurationExtension :
    ExternalSystemReifiedRunConfigurationExtension<GMavenRunConfiguration>(
        GMavenRunConfiguration::class.java
    ) {

    override fun SettingsFragmentsContainer<GMavenRunConfiguration>.configureFragments(
        configuration: GMavenRunConfiguration
    ) {
        val project = configuration.project
        addBeforeRunFragment(GMavenBeforeRunTaskProvider.ID)
        val workingDirectoryField = addWorkingDirectoryFragment(project).component().component
        addCommandLineFragment(project, workingDirectoryField)
    }

    private fun SettingsFragmentsContainer<GMavenRunConfiguration>.addWorkingDirectoryFragment(
        project: Project
    ) = addWorkingDirectoryFragment(
        project,
        ExternalSystemWorkingDirectoryInfo(project, SYSTEM_ID)
    )

    private fun getRawCommandLine(settings: ExternalSystemTaskExecutionSettings): String {
        val commandLine = StringJoiner(" ")
        for (taskName in settings.taskNames) {
            commandLine.add(taskName)
        }
        val scriptParameters = settings.scriptParameters ?: ""
        if (StringUtil.isNotEmpty(scriptParameters)) {
            commandLine.add(scriptParameters)
        }
        return commandLine.toString()
    }

    private fun parseCommandLine(settings: ExternalSystemTaskExecutionSettings, commandLine: String) {
        settings.taskNames = ParametersListUtil.parse(commandLine, true, true)
    }

    private fun SettingsFragmentsContainer<GMavenRunConfiguration>.addCommandLineFragment(
        project: Project,
        workingDirectoryField: WorkingDirectoryField
    ) = addCommandLineFragment(
        project,
        GMavenCommandLineInfo(project, workingDirectoryField),
        { getRawCommandLine(settings) },
        { parseCommandLine(settings, it) }
    )
}
