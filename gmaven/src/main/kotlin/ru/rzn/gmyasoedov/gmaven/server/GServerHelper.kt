@file:JvmName("GServerHelper")

package ru.rzn.gmyasoedov.gmaven.server

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.util.PathUtil
import com.intellij.util.io.isDirectory
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest

fun firstRun(gServerRequest: GServerRequest): MavenResult {
    val request = GServerRequest(
        gServerRequest.taskId,
        gServerRequest.projectPath,
        gServerRequest.mavenPath,
        gServerRequest.sdk,
        nonRecursion = true,
        installGMavenPlugin = true
    )
    val modelRequest = getModelRequest(request)
    val processSupport = GServerRemoteProcessSupport(request)
    try {
        return processSupport.acquire(request.taskId, "", EmptyProgressIndicator()).getProjectModel(modelRequest)
    } catch (e: Exception) {
        MavenLog.LOG.error(e)
        throw RuntimeException(e)
    } finally {
        processSupport.stopAll()
    }
}

fun getProjectModel(request: GServerRequest): MavenResult {
    val processSupport = GServerRemoteProcessSupport(request)
    try {
        val server = processSupport.acquire(request.taskId, "", EmptyProgressIndicator())
        val modelRequest = getModelRequest(request)
        val projectModel = server.getProjectModel(modelRequest)
        if (tryInstallGMavenPlugin(request, projectModel)) {
            firstRun(request)
            return server.getProjectModel(getModelRequest(request))
        }
        return projectModel
    } catch (e: Exception) {
        MavenLog.LOG.error(e)
        throw RuntimeException(e)
    } finally {
        processSupport.stopAll()
    }
}

fun runTasks(request: GServerRequest, tasks: List<String>): MavenResult {
    if (tasks.isEmpty()) {
        throw ExternalSystemException("tasks list is empty");
    }
    if (request.installGMavenPlugin) {
        throw ExternalSystemException("no need install gmaven read model plugin on task execution");
    }
    val modelRequest = getModelRequest(request)
    modelRequest.tasks = tasks;
    val processSupport = GServerRemoteProcessSupport(request)
    try {
        return processSupport.acquire(request.taskId, "", EmptyProgressIndicator()).getProjectModel(modelRequest)
    } catch (e: Exception) {
        MavenLog.LOG.error(e)
        throw RuntimeException(e)
    } finally {
        processSupport.stopAll()
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
    modelRequest.nonRecursion = request.nonRecursion
    if (request.installGMavenPlugin) {
        val clazz = Class.forName("ru.rzn.gmyasoedov.model.reader.DependencyCoordinate")
        modelRequest.gMavenPluginPath = PathUtil.getJarPathForClass(clazz)
    }
    if (request.settings != null) {
        modelRequest.profiles = request.settings.executionWorkspace.profilesData.asSequence()
            .map { it.toRawName() }
            .joinToString(separator = ",")
    }
    return modelRequest;
}