@file:JvmName("GServerHelper2")

package ru.rzn.gmyasoedov.gmaven.server

import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.PathUtil
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.notification.ShowFullLogCallback
import ru.rzn.gmyasoedov.gmaven.project.process.BaseMavenCommandLine
import ru.rzn.gmyasoedov.gmaven.project.process.GOSProcessHandler
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.SnapshotUpdateType
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenException
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenMapResult
import ru.rzn.gmyasoedov.serverapi.GMavenServer
import ru.rzn.gmyasoedov.serverapi.GMavenServer.MAVEN_MODEL_READER_PLUGIN_VERSION
import ru.rzn.gmyasoedov.serverapi.GServerUtils
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.exists

fun getProjectModel2(
    request: GServerRequest,
    processConsumer: ((process: GOSProcessHandler) -> Unit)? = null
): MavenMapResult {
    val baseMavenCommandLine = BaseMavenCommandLine(request)
    val commandLine = baseMavenCommandLine.getCommandLine()
    val workingDirectory = baseMavenCommandLine.workingDirectory
    setupImportParamsFromSettings(request, commandLine)

    printDebugCommandLine(request, commandLine)
    val processHandler = GOSProcessHandler(request, commandLine, processConsumer)
    val mavenResult = runMavenImport(processHandler, workingDirectory)
    if (mavenResult.pluginNotResolved) {
        firstRun(request)
        printDebugCommandLine(request, commandLine)
        val processHandler2 = GOSProcessHandler(request, commandLine, processConsumer)
        return runMavenImport(processHandler2, workingDirectory)
    }
    return mavenResult
}

fun printDebugCommandLine(request: GServerRequest, commandLine: GeneralCommandLine) {
    val parameterList = commandLine.parametersList.getList().dropWhile { it != "-f" }
    val stringBuilder = StringBuilder("mvn")
    for (parameter in parameterList) {
        stringBuilder.append(" $parameter")
    }
    stringBuilder.append(System.lineSeparator()).append(System.lineSeparator())
    request.listener?.onTaskOutput(request.taskId, stringBuilder.toString(), true)
}

fun runTasks2(
    request: GServerRequest,
    tasks: List<String>,
    processConsumer: ((process: GOSProcessHandler) -> Unit)? = null
) {
    if (tasks.isEmpty()) {
        throw ExternalSystemException("tasks list is empty")
    }
    val commandLine = BaseMavenCommandLine(request, false).getCommandLine()
    setupBaseParamsFromSettings(request, commandLine)
    tasks.forEach { commandLine.addParameter(it) }

    val processHandler = GOSProcessHandler(request, commandLine, processConsumer)
    processHandler.startAndWait()
    val exitCode = processHandler.exitCode
    if (exitCode != null && exitCode != 0) {
        throw RuntimeException("See full maven log in run tab")
    }
}

private fun firstRun(request: GServerRequest) {
    try {
        val commandLine = BaseMavenCommandLine(request, false).getCommandLine()
        commandLine.addParameter("-N")
        commandLine.addParameter("install:install-file")
        val clazz = Class.forName("ru.rzn.gmyasoedov.maven.plugin.reader.GAbstractMojo")
        commandLine.addParameter("-Dfile=" + PathUtil.getJarPathForClass(clazz))
        commandLine.addParameter("-DgroupId=ru.rzn.gmyasoedov")
        commandLine.addParameter("-DartifactId=maven-model-reader-plugin")
        commandLine.addParameter("-Dversion=" + MAVEN_MODEL_READER_PLUGIN_VERSION)
        commandLine.addParameter("-Dpackaging=jar")

        val processHandler = GOSProcessHandler(request, commandLine)
        processHandler.startAndWait()
    } catch (e: Exception) {
        MavenLog.LOG.warn(e)
        throw e
    }
}

private fun setupBaseParamsFromSettings(request: GServerRequest, commandLine: GeneralCommandLine) {
    if (request.settings.isNonRecursive) {
        commandLine.addParameter("-N")
    }
    if (request.settings.snapshotUpdateType == SnapshotUpdateType.FORCE) {
        commandLine.addParameter("-U")
    }
    if (request.settings.snapshotUpdateType == SnapshotUpdateType.NEVER) {
        commandLine.addParameter("-nsu")
    }
    if (request.settings.isOfflineWork) {
        commandLine.addParameter("-o")
    }
    if (request.settings.isSkipTests) {
        commandLine.addParameter("-DskipTests")
    }
    if (request.settings.threadCount?.isNotBlank() == true) {
        commandLine.addParameter("-T")
        commandLine.addParameter(request.settings.threadCount!!)
    }
    if (request.settings.outputLevel == ProjectSettingsControlBuilder.OutputLevelType.QUITE) {
        commandLine.addParameter("-q")
    }
    if (request.settings.outputLevel == ProjectSettingsControlBuilder.OutputLevelType.DEBUG) {
        commandLine.addParameter("-X")
    }
    val profiles = request.settings.executionWorkspace.profilesData.map { it.toRawName() }
    if (profiles.isNotEmpty()) {
        commandLine.addParameter("-P")
        commandLine.addParameter(profiles.joinToString(separator = ","))
    }

    val projectList = request.settings.executionWorkspace.projectData.map { it.toRawName() }
    if (projectList.isNotEmpty()) {
        commandLine.addParameter("-pl")
        commandLine.addParameter(projectList.joinToString(separator = ","))

        getSubTaskArgs().forEach { commandLine.addParameter(it) }
    }
    request.settings.arguments.forEach { commandLine.addParameter(it) }
}

private fun setupImportParamsFromSettings(request: GServerRequest, commandLine: GeneralCommandLine) {
    setupBaseParamsFromSettings(request, commandLine)
    request.settings.argumentsImport?.forEach { commandLine.addParameter(it) }
    val importTaskName = "ru.rzn.gmyasoedov:maven-model-reader-plugin:$MAVEN_MODEL_READER_PLUGIN_VERSION:" +
            (if (request.readOnly) "read" else "resolve")
    commandLine.addParameter(importTaskName)
}

private fun runMavenImport(
    processSupport: GOSProcessHandler, workingDirectory: Path,
): MavenMapResult {
    val mavenResult = runMavenImportInner(processSupport, workingDirectory)
    processExceptions(mavenResult.exceptions)
    return mavenResult
}

private fun runMavenImportInner(processSupport: GOSProcessHandler, workingDirectory: Path): MavenMapResult {
    return try {
        processSupport.startAndWait()
        val resultFilePath = getResultFilePath(workingDirectory)
        return FileReader(resultFilePath.toFile(), StandardCharsets.UTF_8).use {
            Gson().fromJson(it, MavenMapResult::class.java)
        }
    } catch (e: Exception) {
        MavenLog.LOG.warn(e)
        GServerUtils.toResult(e)
    }
}

fun getResultFilePath(workingDirectory: Path, fileName: String = GMavenServer.GMAVEN_RESPONSE_POM_FILE): Path {
    return if (workingDirectory.resolve("target").resolve(fileName).exists()) {
        workingDirectory.resolve("target").resolve(fileName)
    } else if (workingDirectory.resolve(fileName).exists()) {
        workingDirectory.resolve(fileName)
    } else {
        throw RuntimeException("Result file not found. See maven log")
    }
}

private fun processExceptions(exceptions: MutableList<MavenException>) {
    if (exceptions.isEmpty()) return
    val errorString = exceptions.joinToString(System.lineSeparator()) { it.message }

    if (Registry.`is`("gmaven.show.full.log")) {
        val fixId = ShowFullLogCallback.ID
        val message = GBundle.message("gmaven.notification.full.log", errorString, fixId)
        throw ExternalSystemException(message, fixId)
    }
    throw ExternalSystemException(errorString)
}

//for tasks
private fun setSubtaskArgs(modelRequest: GetModelRequest) {
    if (modelRequest.projectList.isNotEmpty()) {
        val subTaskArgs = getSubTaskArgs()
        if (subTaskArgs.isNotEmpty()) {
            modelRequest.subTaskArguments = subTaskArgs
        }
    }
}