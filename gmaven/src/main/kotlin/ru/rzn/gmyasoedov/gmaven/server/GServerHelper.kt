@file:JvmName("GServerHelper")

package ru.rzn.gmyasoedov.gmaven.server

import com.intellij.notification.NotificationType
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.util.PathUtil
import com.intellij.util.io.isDirectory
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.settings.OutputLevelType
import ru.rzn.gmyasoedov.gmaven.settings.SnapshotUpdateType
import ru.rzn.gmyasoedov.gmaven.util.GMavenNotification
import ru.rzn.gmyasoedov.gmaven.util.IndicatorUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.serverapi.GMavenServer
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
    modelRequest.importArguments = null
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
    modelRequest.importArguments = null
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
    modelRequest.dependencyAnalyzerGA = artifactGA
    val processSupport = GServerRemoteProcessSupport(request)
    try {
        var taskInfo = IndicatorUtil.getTaskInfo(GBundle.message("gmaven.dependency.tree.title"), false)
        var indicator = BackgroundableProcessIndicator(taskInfo)
        var mavenResult = runMavenTaskInner(processSupport, modelRequest, indicator, taskInfo)
        if (couldNotFindSelectedProject(mavenResult)) {
            taskInfo = IndicatorUtil.getTaskInfo(GBundle.message("gmaven.dependency.tree.title"), false)
            indicator = BackgroundableProcessIndicator(taskInfo)
            modelRequest.dependencyAnalyzerGA = GMavenServer.RESOLVE_TASK
            mavenResult = runMavenTask(GServerRemoteProcessSupport(request), modelRequest, indicator, taskInfo)
            val message = GBundle.message("gmaven.dependency.tree.resolve.warning", artifactGA)
            GMavenNotification.createNotificationDA(message, NotificationType.WARNING)
        }
        return mavenResult.projectContainer?.modules?.map { it.project } ?: emptyList()
    } catch (e: Exception) {
        MavenLog.LOG.warn(e)
        GMavenNotification.createNotificationDA(e.localizedMessage, NotificationType.ERROR)
        return emptyList()
    }
}

private fun couldNotFindSelectedProject(mavenResult: MavenResult): Boolean {
    return mavenResult.exceptions?.isNotEmpty() ?: false
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
    modelRequest.updateSnapshots = request.settings.snapshotUpdateType == SnapshotUpdateType.FORCE
    modelRequest.notUpdateSnapshots = request.settings.snapshotUpdateType == SnapshotUpdateType.NEVER
    modelRequest.offline = request.settings.isOfflineWork
    modelRequest.threadCount = request.settings.threadCount
    modelRequest.quiteLogs = request.settings.outputLevel == OutputLevelType.QUITE
    modelRequest.debugLog = request.settings.outputLevel == OutputLevelType.DEBUG
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
    modelRequest.importArguments = request.settings.argumentsImport
    return modelRequest
}

private fun runMavenTask(processSupport: GServerRemoteProcessSupport,
                         modelRequest: GetModelRequest,
                         indicator: ProgressIndicator = EmptyProgressIndicator(),
                         taskInfo: Task.Backgroundable? = null): MavenResult {
    val mavenResult = runMavenTaskInner(processSupport, modelRequest, indicator, taskInfo)
    processExceptions(mavenResult.exceptions)
    return mavenResult
}

private fun runMavenTaskInner(
    processSupport: GServerRemoteProcessSupport,
    modelRequest: GetModelRequest,
    indicator: ProgressIndicator = EmptyProgressIndicator(),
    taskInfo: Task.Backgroundable? = null
): MavenResult {
    try {
        val projectModel = processSupport.acquire(processSupport.id, "", indicator)
            .getProjectModel(modelRequest)
        return GServerUtils.toResult(projectModel)
    } catch (e: Exception) {
        MavenLog.LOG.error(e)
        return GServerUtils.toResult(e)
    } finally {
        processSupport.stopAll()
        if (taskInfo != null && indicator is BackgroundableProcessIndicator) {
            indicator.finish(taskInfo)
        }
    }
}

private fun processExceptions(exceptions: MutableList<MavenException>) {
    if (exceptions.isEmpty()) return
    val errorString = exceptions.joinToString(System.lineSeparator()) { it.message }
    throw ExternalSystemException(errorString)
}