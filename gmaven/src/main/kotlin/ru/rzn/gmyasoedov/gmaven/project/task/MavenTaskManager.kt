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
import org.jetbrains.annotations.VisibleForTesting
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle.message
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver
import ru.rzn.gmyasoedov.gmaven.project.getMavenHome
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.runTasks2
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.gmaven.util.GMavenNotification
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.notExists

class MavenTaskManager : ExternalSystemTaskManager<MavenExecutionSettings> {
    private val cancellationMap = ConcurrentHashMap<ExternalSystemTaskId, Any>()

    override fun executeTasks(
        id: ExternalSystemTaskId,
        taskNames: MutableList<String>,
        projectPath: String,
        settings: MavenExecutionSettings?,
        jvmParametersSetup: String?,
        listener: ExternalSystemTaskNotificationListener
    ) {
        settings ?: throw ExternalSystemException("settings is empty")

        val tasks = getTasks(taskNames, settings)
        val workspace = settings.executionWorkspace
        val projectBuildFile = workspace.projectBuildFile
            ?: throw ExternalSystemException("project build file is empty")
        val buildPath = Path.of(workspace.subProjectBuildFile ?: projectBuildFile)

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
        val mavenHome = getMavenHome(settings)
        try {
            val request = GServerRequest(id, buildPath, mavenHome, sdk, settings, listener = listener)
            runTasks2(request, tasks) { cancellationMap[id] = it }
        } finally {
            cancellationMap.remove(id)
        }
    }

    private fun getTasks(taskNames: List<String>, settings: MavenExecutionSettings): List<String> {
        val tasks = if (Registry.stringValue("gmaven.lifecycles").contains(" ")) {
            taskNames.flatMap { it.split(" ") }
        } else {
            taskNames
        }
        return preprocessSettingsAndOrder(tasks, settings)
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        MavenProjectResolver.cancelTask(id, cancellationMap)
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

    private fun preprocessSettingsAndOrder(taskNames: List<String>, settings: MavenExecutionSettings): List<String> {
        if (taskNames.size == 3 && taskNames[0] == GMavenConstants.TASK_EFFECTIVE_POM) {
            if (taskNames[taskNames.size - 2] != "-f") return taskNames
            val buildFile = taskNames[taskNames.size - 1]
            if (Path(buildFile).notExists()) return taskNames
            settings.executionWorkspace.subProjectBuildFile = buildFile
            return listOf(taskNames[0])
        }
        return prepareTaskOrder(taskNames)
    }

    @VisibleForTesting
    fun prepareTaskOrder(taskNames: List<String>): List<String> {
        if (taskNames.size < 2) return taskNames
        val phaseTasks = TreeMap<Int, String>()
        val otherTasks = mutableListOf<String>()
        for (taskName in taskNames) {
            val phase = Phase.find(taskName)
            if (phase != null) phaseTasks[phase.ordinal] = phase.phaseName else otherTasks.add(taskName)
        }
        return phaseTasks.values + otherTasks
    }
}