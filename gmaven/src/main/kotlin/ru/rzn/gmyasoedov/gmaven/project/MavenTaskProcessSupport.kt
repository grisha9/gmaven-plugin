package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.registry.Registry
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.MavenServerCmdState
import ru.rzn.gmyasoedov.serverapi.GMavenServer
import java.nio.file.Path


class MavenTaskProcessSupport(private val request: GServerRequest, private val isImport: Boolean = true) {
    private val workingDirectory = getWorkingDirectory(request)
    private val jvmConfigOptions = GServerRemoteProcessSupport.getJvmConfigOptions(workingDirectory)
//QualityToolProcessHandler
    fun getCommandLine(): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        setupDebugParam(commandLine)
        setupGmavenPluginsProperty(commandLine)
     //   processVmOptions(commandLine)
        commandLine.exePath = "/home/Grigoriy.Myasoedov/.sdkman/candidates/maven/3.9.1/bin/mvn"
        commandLine.exePath = "/home/Grigoriy.Myasoedov/.sdkman/candidates/mvnd/1.0-m8-m39/bin/mvnd.sh"
        commandLine.addParameter("clean")
        commandLine.addParameter("package")
        commandLine.workDirectory = workingDirectory.toFile()
        commandLine.isRedirectErrorStream = true

        commandLine.addParameter("-f")
        commandLine.addParameter("/home/Grigoriy.Myasoedov/jb/single-pom")
        commandLine.addParameter("-DskipTests")
        commandLine.environment["JAVA_HOME"] = request.settings.javaHome
        //commandLine.environment["-DskipTests"] = "true"
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
        for (plugin in extensionList) {
            pluginsForImport.add(plugin.key)
        }

        if (pluginsForImport.isNotEmpty()) {
            params.environment[GMavenServer.GMAVEN_PLUGINS] = pluginsForImport.joinToString(",")
        }
    }

    private fun processVmOptions(params: GeneralCommandLine) {
        val vmOptions: MutableList<String> = ArrayList(this.jvmConfigOptions)
        vmOptions.addAll(request.settings.jvmArguments)
        for (param in vmOptions) {
            if (isImport && Registry.`is`("gmaven.vm.remove.javaagent") && param.startsWith("-javaagent")) {
                continue
            }
            params.addParameter(param)
        }
    }

    private fun getWorkingDirectory(request: GServerRequest): Path {
        return if (request.projectPath.toFile().isDirectory) request.projectPath else request.projectPath.parent
    }
}