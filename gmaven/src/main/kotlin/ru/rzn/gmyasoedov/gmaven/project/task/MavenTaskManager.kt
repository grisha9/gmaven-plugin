package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProjectJdkNotFoundException
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.util.containers.ContainerUtil
import ru.rzn.gmyasoedov.gmaven.project.getMavenHome
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.runTasks
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import java.nio.file.Path

class MavenTaskManager : ExternalSystemTaskManager<MavenExecutionSettings> {

    override fun executeTasks(
        id: ExternalSystemTaskId,
        taskNames: MutableList<String>,
        projectPath: String,
        settings: MavenExecutionSettings?,
        jvmParametersSetup: String?,
        listener: ExternalSystemTaskNotificationListener
    ) {
        val sdk = (settings ?: throw ExternalSystemException("settings is empty"))
            .jdkName?.let { ExternalSystemJdkUtil.getJdk(null, it) }
            ?: throw ProjectJdkNotFoundException() //InvalidJavaHomeException
        val mavenHome = getMavenHome(settings.distributionSettings)

        val projectBuildFile = settings.projectBuildFile ?: throw ExternalSystemException("project build file is empty")
        val subProjectBuildFile = settings.subProjectBuildFile
        if (subProjectBuildFile == null) {
            val buildPath = Path.of(projectBuildFile)
            val request = GServerRequest(id, buildPath, mavenHome, sdk, settings, listener = listener)
            val mavenResult = runTasks(request, taskNames, null);
            if (!ContainerUtil.isEmpty(mavenResult.exceptions)) {
                throw ExternalSystemException()
            }
        } else {
            val buildPath = if (settings.executionWorkspace.artifactGA == null)
                Path.of(subProjectBuildFile) else Path.of(projectBuildFile)
            val request = GServerRequest(id, buildPath, mavenHome, sdk, settings, listener = listener)
            val mavenResult = runTasks(request, taskNames, settings.executionWorkspace.artifactGA);
            if (!ContainerUtil.isEmpty(mavenResult.exceptions)) {
                throw ExternalSystemException()
            }
        }
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        TODO("Not yet implemented")
    }
}