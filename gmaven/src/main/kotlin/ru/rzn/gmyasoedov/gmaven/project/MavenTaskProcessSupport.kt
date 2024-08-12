package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.MavenServerCmdState
import java.nio.file.Path
import kotlin.io.path.absolutePathString


class MavenTaskProcessSupport(private val request: GServerRequest, private val isImport: Boolean = true) {
    private val workingDirectory = getWorkingDirectory(request)
//QualityToolProcessHandler
    fun getCommandLine(): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        setupDebugParam(commandLine)
        setupGmavenPluginsProperty(commandLine)
        setupMavenOpts(commandLine)
        commandLine.exePath = request.mavenPath.resolve("bin").resolve("mvn").absolutePathString()
        commandLine.addParameter("ru.rzn.gmyasoedov:maven-model-reader-plugin:1.0-SNAPSHOT:resolve")
        commandLine.workDirectory = workingDirectory.toFile()
        commandLine.isRedirectErrorStream = true

        commandLine.addParameter("-f")
        commandLine.addParameter(workingDirectory.absolutePathString())
        commandLine.environment["JAVA_HOME"] = request.settings.javaHome
        commandLine.addParameter("-DresultAsTree=true")
        return commandLine
    }

    private fun setupDebugParam(commandLine: GeneralCommandLine) {
        val debugPort = MavenServerCmdState.getDebugPort() ?: return
        commandLine.addParameter("-Xdebug")
        commandLine.addParameter("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:$debugPort")
    }

    private fun setupGmavenPluginsProperty(params: GeneralCommandLine) {
        val extensionList = MavenFullImportPlugin.EP_NAME.extensionList
        val pluginsForImport: MutableList<String> = ArrayList(extensionList.size)
        val pluginsForResolve: MutableList<String> = ArrayList(extensionList.size)
        for (plugin in extensionList) {
            pluginsForImport.add(plugin.key)
            if ((plugin as? MavenCompilerFullImportPlugin)?.resolvePlugin() == true) {
                pluginsForResolve.add(plugin.getArtifactId())
            }
        }

        if (pluginsForImport.isNotEmpty()) {
            params.addParameter("-DprocessingPluginGAIds=${createListParameter(pluginsForImport)}")
            params.addParameter("-DresolvedPluginGAIds=${createListParameter(pluginsForResolve)}")
        }
    }

    private fun createListParameter(pluginsForImport: MutableList<String>) =
        ParametersListUtil.escape(pluginsForImport.joinToString(","))

    private fun setupMavenOpts(commandLine: GeneralCommandLine) {
        var mavenOpts = System.getenv("MAVEN_OPTS") ?: ""
        if (request.settings.jvmArguments.isNotEmpty()) {
            mavenOpts += ParametersListUtil.join(request.settings.jvmArguments)
        }
        if (mavenOpts.isNotBlank()) {
            commandLine.environment["MAVEN_OPTS"] = mavenOpts
        }
    }

    private fun getWorkingDirectory(request: GServerRequest): Path {
        return if (request.projectPath.toFile().isDirectory) request.projectPath else request.projectPath.parent
    }
}