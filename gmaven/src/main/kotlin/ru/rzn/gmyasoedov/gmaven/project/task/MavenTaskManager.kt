package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProjectJdkNotFoundException
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import ru.rzn.gmyasoedov.gmaven.project.getMavenHome
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.runTasks
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class MavenTaskManager : ExternalSystemTaskManager<MavenExecutionSettings> {
    private val cancellationMap = ConcurrentHashMap<ExternalSystemTaskId, GServerRemoteProcessSupport>()

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

        val workspace = settings.executionWorkspace
        val projectBuildFile = workspace.projectBuildFile
            ?: throw ExternalSystemException("project build file is empty")
        val subProjectBuildFile = workspace.subProjectBuildFile
        try {
            val buildPath = Path.of(subProjectBuildFile ?: projectBuildFile)
            val request = GServerRequest(id, buildPath, mavenHome, sdk, settings, listener = listener)
            runTasks(request, taskNames) { cancellationMap[id] = it }
        } finally {
            cancellationMap.remove(id)
        }
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        cancellationMap[id]?.stopAll()
        return true
    }
}