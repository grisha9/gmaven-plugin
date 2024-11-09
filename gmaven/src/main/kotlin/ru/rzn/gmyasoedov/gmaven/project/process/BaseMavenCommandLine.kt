package ru.rzn.gmyasoedov.gmaven.project.process

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.execution.ParametersListUtil
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.MavenServerCmdState
import ru.rzn.gmyasoedov.gmaven.util.MavenPathUtil
import ru.rzn.gmyasoedov.serverapi.GMavenServer
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name


class BaseMavenCommandLine(private val request: GServerRequest, private val isImport: Boolean = true) {
    private val workingDirectory = getWorkingDirectory(request)
    val resultFilePath: Path = detectResultFilePath()

    //QualityToolProcessHandler
    fun getCommandLine(): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        commandLine.environment["JAVA_HOME"] = request.settings.javaHome
        setupDebugParam(commandLine)
        setupGmavenPluginsProperty(commandLine)
        setupMavenOpts(commandLine)
        setupProjectPath(commandLine, request)

        commandLine.exePath = getExeMavenPath()
        commandLine.workDirectory = workingDirectory.toFile()
        commandLine.isRedirectErrorStream = true

        return commandLine
    }

    private fun setupProjectPath(commandLine: GeneralCommandLine, request: GServerRequest) {
        commandLine.addParameters("-f", request.projectPath.absolutePathString())
    }

    private fun getExeMavenPath(): String {
        val mavenPath = request.mavenPath
        if (mavenPath.name == "mvnw" || mavenPath.name == "mvnw.cmd") return mavenPath.absolutePathString()
        if (mavenPath.name == "mvnd.sh" || mavenPath.name == "mvnd.cmd") return mavenPath.absolutePathString()
        return if (SystemInfo.isWindows) getWinPath(mavenPath) else getUnixPath(mavenPath)
    }

    private fun getUnixPath(mavenPath: Path): String {
        val basePath = mavenPath.resolve("bin")
        if (basePath.resolve("mvnd.sh").exists()) {
            return basePath.resolve("mvnd.sh").absolutePathString()
        }
        if (basePath.resolve("mvn").exists()) {
            return basePath.resolve("mvn").absolutePathString()
        }
        return basePath.resolve("mvn.sh").absolutePathString()
    }

    private fun getWinPath(mavenPath: Path): String {
        val basePath = mavenPath.resolve("bin")
        if (basePath.resolve("mvnd.cmd").exists()) {
            return basePath.resolve("mvnd.cmd").absolutePathString()
        }
        if (basePath.resolve("mvn.cmd").exists()) {
            return basePath.resolve("mvn.cmd").absolutePathString()
        }
        return basePath.resolve("mvn.bat").absolutePathString()
    }

    private fun setupDebugParam(commandLine: GeneralCommandLine) {
        if (Registry.`is`("gmaven.process.jsonPrettyPrinting")) {
            commandLine.parametersList.addProperty("jsonPrettyPrinting", "true")
        }
        val debugPort = MavenServerCmdState.getDebugPort() ?: return
        commandLine.parametersList.addProperty("jsonPrettyPrinting", "true")
        commandLine.addParameter("-Xdebug")
        commandLine.addParameter("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:$debugPort")
    }

    private fun setupGmavenPluginsProperty(params: GeneralCommandLine) {
        if (!isImport) return
        params.parametersList.addProperty("maven.ext.class.path", MavenPathUtil.getExtClassesJarPathString())
        params.parametersList.addProperty("resultAsTree", "true")
        params.parametersList.addProperty("resultFilePath", resultFilePath.absolutePathString())
        if (!request.settings.isShowPluginNodes) {
            params.parametersList.addProperty("allPluginsInfo", "false")
        }
        val extensionList = MavenFullImportPlugin.EP_NAME.extensionList
        val pluginsForImport: MutableList<String> = ArrayList(extensionList.size)
        val pluginsForResolve: MutableList<String> = ArrayList(extensionList.size)
        for (plugin in extensionList) {
            pluginsForImport.add(plugin.key)
            if ((plugin as? MavenCompilerFullImportPlugin)?.resolvePlugin() == true) {
                pluginsForResolve.add(plugin.key)
            }
        }

        if (pluginsForImport.isNotEmpty()) {
            params.addParameter("-DprocessingPluginGAIds=${createListParameter(pluginsForImport)}")
            params.addParameter("-DresolvedPluginGAIds=${createListParameter(pluginsForResolve)}")
        }
    }

    private fun createListParameter(plugins: MutableList<String>) = plugins.joinToString(",")

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

    private fun detectResultFilePath(): Path {
        val targetPath = workingDirectory.resolve("target")
        if (targetPath.toFile().exists()) {
            return targetPath.resolve(GMavenServer.GMAVEN_RESPONSE_POM_FILE)
        }
        return workingDirectory.resolve(GMavenServer.GMAVEN_RESPONSE_POM_FILE)
    }
}