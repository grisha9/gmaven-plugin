package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.ide.actions.ShowLogAction
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProjectJdkNotFoundException
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.openapi.util.registry.Registry
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle.message
import ru.rzn.gmyasoedov.gmaven.project.getMavenHome
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.runTasks
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.gmaven.util.GMavenNotification
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
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
        settings ?: throw ExternalSystemException("settings is empty")
        val workspace = settings.executionWorkspace
        val projectBuildFile = workspace.projectBuildFile
            ?: throw ExternalSystemException("project build file is empty")
        val buildPath = Path.of(workspace.subProjectBuildFile ?: projectBuildFile)
        val tasks = getTasks(taskNames)

        if (settings.isUseMvndForTasks) {
            try {
                MvndTaskManager.executeTasks(settings, buildPath, tasks)
                return
            } catch (e: Throwable) {
                processErrorAndShowNotify(e)
            }
        }
        val sdk = settings.jdkName?.let { ExternalSystemJdkUtil.getJdk(null, it) }
            ?: throw ProjectJdkNotFoundException() //InvalidJavaHomeException
        val mavenHome = getMavenHome(settings.distributionSettings)
        try {
            val request = GServerRequest(id, buildPath, mavenHome, sdk, settings, listener = listener)
            runTasks(request, tasks) { cancellationMap[id] = it }
        } finally {
            cancellationMap.remove(id)
        }
    }

    private fun getTasks(taskNames: MutableList<String>): List<String> {
        return if (Registry.stringValue("gmaven.lifecycles").contains(" ")) {
            taskNames.flatMap { it.split(" ") }
        } else {
            taskNames
        }
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        cancellationMap[id]?.stopAll()
        return true
    }

    private fun processErrorAndShowNotify(e: Throwable) {
        MavenLog.LOG.warn(e)
        GMavenNotification.createNotification(
            message("gmaven.mvnd.notification.title"),
            message("gmaven.mvnd.notification.error"),
            WARNING,
            listOf(ActionManager.getInstance().getAction("OpenLog"), ShowLogAction.notificationAction())
        )
    }
}