package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.execution.ParametersListUtil
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.MavenServerCmdState
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name


class MavenTaskProcessSupport(private val request: GServerRequest, private val isImport: Boolean = true) {
    private val workingDirectory = getWorkingDirectory(request)

    //QualityToolProcessHandler
    fun getCommandLine(): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        setupDebugParam(commandLine)
        setupGmavenPluginsProperty(commandLine)
        setupMavenOpts(commandLine)
        commandLine.exePath = getExeMavenPath()

        commandLine.workDirectory = workingDirectory.toFile()
        commandLine.isRedirectErrorStream = true

        /*commandLine.addParameter("ru.rzn.gmyasoedov:maven-model-reader-plugin:1.0-SNAPSHOT:resolve")
        commandLine.addParameter("-f")
        commandLine.addParameter(workingDirectory.absolutePathString())*/
        commandLine.addParameter("-DresultAsTree=true")
        commandLine.environment["JAVA_HOME"] = request.settings.javaHome
        return commandLine
    }

    private fun getExeMavenPath(): String {
        val mavenPath = request.mavenPath
        if (mavenPath.name == "mvnw" || mavenPath.name == "mvnw.cmd") return mavenPath.absolutePathString()
        if (mavenPath.name == "mvnd.sh" || mavenPath.name == "mvnd.cmd") return mavenPath.absolutePathString()
        return if (SystemInfo.isWindows) getWinPath(mavenPath) else getUnixPath(mavenPath)
    }

    private fun getUnixPath(mavenPath: Path): String {
        val basePath = mavenPath.resolve("bin")
        if (basePath.resolve("mvn").exists()) {
            return basePath.resolve("mvn").absolutePathString()
        }
        return basePath.resolve("mvn.sh").absolutePathString()
    }

    private fun getWinPath(mavenPath: Path): String {
        val basePath = mavenPath.resolve("bin")
        if (basePath.resolve("mvn.cmd").exists()) {
            return basePath.resolve("mvn.cmd").absolutePathString()
        }
        return basePath.resolve("mvn.bat").absolutePathString()
    }

    private fun setupDebugParam(commandLine: GeneralCommandLine) {
        val debugPort = MavenServerCmdState.getDebugPort() ?: return
        commandLine.addParameter("-Xdebug")
        commandLine.addParameter("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:$debugPort")
    }

    private fun setupGmavenPluginsProperty(params: GeneralCommandLine) {
        if (!isImport) return
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