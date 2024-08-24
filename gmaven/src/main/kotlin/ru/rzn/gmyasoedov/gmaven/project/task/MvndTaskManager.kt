package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunConfiguration
import ru.rzn.gmyasoedov.gmaven.server.getSubTaskArgs
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.nio.file.Path

object MvndTaskManager {

    @Throws(IllegalStateException::class, ExecutionException::class)
    fun executeTasks(settings: MavenExecutionSettings, buildPath: Path, tasks: List<String>) {
        val projectDirectory = if (buildPath.toFile().isDirectory()) buildPath else buildPath.parent
        val parametersList = getParametersList(buildPath, settings, tasks)
        val project = getProject(settings)

        val mvndCommand = project.getService(TerminalMvndService::class.java)
            ?.getMvndPath()
            ?: throw IllegalStateException("Terminal plugin is disabled")
        val runConfiguration = project.getService(ShMvndService::class.java)
            ?.getRunConfiguration(project, mvndCommand, parametersList, projectDirectory)
            ?: throw IllegalStateException("Sh plugin is disabled")

        val builder = ExecutionEnvironmentBuilder
            .createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration)
            ?: throw IllegalStateException("ExecutionEnvironmentBuilder not found")
        ExecutionManager.getInstance(project).restartRunProfile(builder.build())
    }

    private fun getProject(settings: MavenExecutionSettings): Project {
        val projectPath = settings.executionWorkspace.externalProjectPath
            ?: throw IllegalStateException("No external project path")
        return ProjectManager.getInstance().openProjects.find { MavenUtils.equalsPaths(it.basePath, projectPath) }
            ?: throw IllegalStateException("Project not found by $projectPath")
    }

    private fun getParametersList(
        projectPath: Path, settings: MavenExecutionSettings, tasks: List<String>
    ): ParametersList {
        val parametersList = ParametersList()
        if (!projectPath.toFile().isDirectory()) {
            parametersList.add("-f")
            parametersList.add(projectPath.toString())
        }
        if (settings.isNonRecursive) {
            parametersList.add("-N")
        }
        if (settings.snapshotUpdateType == ProjectSettingsControlBuilder.SnapshotUpdateType.FORCE) {
            parametersList.add("-U")
        }
        if (settings.snapshotUpdateType == ProjectSettingsControlBuilder.SnapshotUpdateType.NEVER) {
            parametersList.add("-nsu")
        }
        if (settings.isOfflineWork) {
            parametersList.add("-o")
        }
        if (StringUtil.isNotEmpty(settings.threadCount)) {
            parametersList.add("-T")
            parametersList.add(settings.threadCount)
        }
        if (settings.outputLevel == ProjectSettingsControlBuilder.OutputLevelType.QUITE) {
            parametersList.add("-q")
        }
        if (settings.outputLevel == ProjectSettingsControlBuilder.OutputLevelType.DEBUG) {
            parametersList.add("-X")
        }

        val profiles = settings.executionWorkspace.profilesData.asSequence()
            .map { it.toRawName() }
            .joinToString(separator = ",")
        if (StringUtil.isNotEmpty(profiles)) {
            parametersList.add("-P")
            parametersList.add(profiles)
        }
        val projectList = settings.executionWorkspace.projectData.asSequence()
            .map { it.toRawName() }
            .joinToString(separator = ",")
        if (StringUtil.isNotEmpty(projectList)) {
            parametersList.add("-pl")
            parametersList.add(projectList)
            parametersList.addAll(getSubTaskArgs())
        }
        settings.arguments.forEach { parametersList.add(it) }
        tasks.forEach { parametersList.add(it) }
        settings.env
            .forEach { (k, v) -> if (v != null) parametersList.addProperty(k, v) else parametersList.addProperty(k) }
        return parametersList
    }
}

class TerminalMvndService {
    fun getMvndPath() = Registry.stringValue("gmaven.mvnd.path")
}

class ShMvndService {
    fun getRunConfiguration(
        project: Project, mvndCommand: String, parametersList: ParametersList, projectDirectory: Path
    ): ShRunConfiguration {
        val configurationSettings = RunManager.getInstance(project)
            .createConfiguration("GMaven mvnd", ShConfigurationType::class.java)
        val configuration = configurationSettings.configuration
        if (configuration !is ShRunConfiguration) {
            throw IllegalStateException("Configuration not found")
        }
        val runConfiguration = configurationSettings.configuration as ShRunConfiguration
        runConfiguration.scriptPath = mvndCommand
        runConfiguration.scriptOptions = parametersList.parametersString
        runConfiguration.isExecuteScriptFile = true
        runConfiguration.scriptWorkingDirectory = projectDirectory.toString()
        runConfiguration.interpreterPath = ""
        return runConfiguration;
    }
}