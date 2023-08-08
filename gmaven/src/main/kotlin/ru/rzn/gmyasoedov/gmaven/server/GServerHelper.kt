@file:JvmName("GServerHelper")

package ru.rzn.gmyasoedov.gmaven.server

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.util.PathUtil
import com.intellij.util.io.isDirectory
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.serverapi.GServerUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenException
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest

fun firstRun(gServerRequest: GServerRequest): MavenResult {
    val request = GServerRequest(
        gServerRequest.taskId,
        gServerRequest.projectPath,
        gServerRequest.mavenPath,
        gServerRequest.sdk,
        gServerRequest.settings,
        installGMavenPlugin = true
    )
    val modelRequest = getModelRequest(request)
    val processSupport = GServerRemoteProcessSupport(request)
    return runMavenTask(processSupport, modelRequest)
}

fun getProjectModel(
    request: GServerRequest,
    processConsumer: ((process: GServerRemoteProcessSupport) -> Unit)? = null
): MavenResult {
    val modelRequest = getModelRequest(request)
    var processSupport = GServerRemoteProcessSupport(request)
    processConsumer?.let { it(processSupport) }
    val mavenResult = runMavenTask(processSupport, modelRequest)
    if (tryInstallGMavenPlugin(request, mavenResult)) {
        firstRun(request)
        processSupport = GServerRemoteProcessSupport(request)
        processConsumer?.let { it(processSupport) }
        return runMavenTask(processSupport, modelRequest)
    }
    return mavenResult;
}

fun runTasks(
    request: GServerRequest,
    tasks: List<String>,
    processConsumer: ((process: GServerRemoteProcessSupport) -> Unit)? = null
): MavenResult {
    if (tasks.isEmpty()) {
        throw ExternalSystemException("tasks list is empty")
    }
    if (request.installGMavenPlugin) {
        throw ExternalSystemException("no need install gmaven read model plugin on task execution")
    }
    val modelRequest = getModelRequest(request)
    modelRequest.tasks = tasks
    val processSupport = GServerRemoteProcessSupport(request)
    processConsumer?.let { it(processSupport) }
    return runMavenTask(processSupport, modelRequest)
}

fun getDependencyTree(gServerRequest: GServerRequest, artifactGA: String): List<MavenProject> {
    val request = GServerRequest(
        gServerRequest.taskId,
        gServerRequest.projectPath,
        gServerRequest.mavenPath,
        gServerRequest.sdk,
        gServerRequest.settings
    )
    val modelRequest = getModelRequest(request)
    modelRequest.analyzerGA = artifactGA
    val processSupport = GServerRemoteProcessSupport(request)
    try {
        val mavenResult = runMavenTask(processSupport, modelRequest)
        return mavenResult.projectContainer?.modules?.map { it.project } ?: emptyList()
    } catch (e: Exception) {
        MavenLog.LOG.warn(e)
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GMavenConstants.SYSTEM_ID.readableName)
            ?.let { ApplicationManager.getApplication().invokeLater {
                it.createNotification(
                    GBundle.message("gmaven.dependency.tree.title"), e.localizedMessage, NotificationType.ERROR
                ).notify(null)
            } }
        return emptyList()
    }
}

private fun tryInstallGMavenPlugin(request: GServerRequest, mavenResult: MavenResult) =
    !request.installGMavenPlugin && mavenResult.pluginNotResolved

private fun getModelRequest(request: GServerRequest): GetModelRequest {
    val projectPath = request.projectPath
    val directory = projectPath.isDirectory()
    val projectDirectory = if (directory) projectPath else projectPath.parent

    val modelRequest = GetModelRequest()
    modelRequest.projectPath = projectDirectory.toString()
    modelRequest.alternativePom = if (directory) null else projectPath.toString()
    modelRequest.nonRecursion = request.settings.isNonRecursive
    modelRequest.updateSnapshots = request.settings.isUpdateSnapshots
    modelRequest.offline = request.settings.isOfflineWork
    modelRequest.threadCount = request.settings.threadCount
    modelRequest.quiteLogs = request.settings.outputLevel == ProjectSettingsControlBuilder.OutputLevelType.QUITE
    modelRequest.debugLog = request.settings.outputLevel == ProjectSettingsControlBuilder.OutputLevelType.DEBUG
    if (request.installGMavenPlugin) {
        val clazz = Class.forName("ru.rzn.gmyasoedov.model.reader.DependencyCoordinate")
        modelRequest.gMavenPluginPath = PathUtil.getJarPathForClass(clazz)
        modelRequest.nonRecursion = true
    }
    modelRequest.profiles = request.settings.executionWorkspace.profilesData.asSequence()
        .map { it.toRawName() }
        .joinToString(separator = ",")

    modelRequest.projectList = request.settings.executionWorkspace.projectData.asSequence()
        .map { it.toRawName() }
        .joinToString(separator = ",")
    modelRequest.additionalArguments = request.settings.arguments
    return modelRequest
}

private fun runMavenTask(processSupport: GServerRemoteProcessSupport, modelRequest: GetModelRequest): MavenResult {
    val mavenResult = runMavenTaskInner(processSupport, modelRequest)
    processExceptions(mavenResult.exceptions)
    return mavenResult
}

private fun runMavenTaskInner(
    processSupport: GServerRemoteProcessSupport,
    modelRequest: GetModelRequest
): MavenResult {
    try {
        val projectModel = processSupport.acquire(processSupport.id, "", EmptyProgressIndicator())
            .getProjectModel(modelRequest)
        return GServerUtils.toResult(projectModel)
    } catch (e: Exception) {
        MavenLog.LOG.error(e)
        return GServerUtils.toResult(e)
    } finally {
        processSupport.stopAll()
    }
}

private fun processExceptions(exceptions: MutableList<MavenException>) {
    if (exceptions.isEmpty()) return
    val errorString = exceptions.joinToString(System.lineSeparator()) { it.message }
    throw ExternalSystemException(errorString)
}