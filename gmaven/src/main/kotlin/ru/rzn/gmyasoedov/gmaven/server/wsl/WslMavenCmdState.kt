package ru.rzn.gmyasoedov.gmaven.server.wsl

import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.rmi.RemoteServer
import com.intellij.execution.target.TargetProgressIndicator
import com.intellij.execution.target.java.JavaLanguageRuntimeConfiguration
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.target.WslTargetEnvironmentConfiguration
import com.intellij.execution.wsl.target.WslTargetEnvironmentRequest
import com.intellij.openapi.projectRoots.Sdk
import ru.rzn.gmyasoedov.gmaven.server.MavenServerCmdState
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.serverapi.GMavenServer
import ru.rzn.gmyasoedov.serverapi.GMavenServer.GMAVEN_HOME
import ru.rzn.gmyasoedov.serverapi.GMavenServer.MAVEN_EXT_CLASS_PATH_PROPERTY
import java.nio.file.Path

internal class WslMavenCmdState(
    private val wslDistribution: WSLDistribution,
    jdk: Sdk,
    mavenPath: Path,
    workingDirectory: Path,
    jvmConfigOptions: List<String>,
    executionSettings: MavenExecutionSettings,
) : MavenServerCmdState(jdk, mavenPath, workingDirectory, jvmConfigOptions, executionSettings) {

    override fun getMavenOptions() = wslDistribution.environment?.get("MAVEN_OPTS")

    override fun createJavaParameters(): SimpleJavaParameters {
        val hostAddress = wslDistribution.getWslIpAddress().hostAddress
        val parameters = super.createJavaParameters()
        val wslParams = toWslParameters(parameters)
        wslParams.vmParametersList.addProperty(RemoteServer.SERVER_HOSTNAME, hostAddress)
        wslParams.vmParametersList.addProperty(GMavenServer.SERVER_WSL_PROPERTY, "true")
        wslParams.vmParametersList.addProperty(GMAVEN_HOME, wslDistribution.getWslPath(mavenPath.toString()))
        wslParams.vmParametersList.getPropertyValue(MAVEN_EXT_CLASS_PATH_PROPERTY)?.also {
            wslParams.vmParametersList.addProperty(MAVEN_EXT_CLASS_PATH_PROPERTY, wslDistribution.getWslPath(it))
        }
        return wslParams
    }

    private fun toWslParameters(parameters: SimpleJavaParameters): SimpleJavaParameters {
        val wslParams = SimpleJavaParameters()
        wslParams.mainClass = parameters.mainClass
        for (item in parameters.vmParametersList.parameters) {
            wslParams.vmParametersList.add(item)
        }
        for (item in parameters.programParametersList.parameters) {
            wslParams.programParametersList.add(item)
        }
        wslParams.charset = parameters.charset
        wslParams.vmParametersList.add("-classpath")
        wslParams.vmParametersList
            .add(parameters.classPath.pathList.mapNotNull(wslDistribution::getWslPath).joinToString(":"))
        return wslParams
    }

    override fun startProcess(): ProcessHandler {
        val wslConfig = WslTargetEnvironmentConfiguration(wslDistribution)
        val request = WslTargetEnvironmentRequest(wslConfig)

        val wslParams = createJavaParameters()
        val languageRuntime = JavaLanguageRuntimeConfiguration()

        val wslPath = jdk.homePath?.let(wslDistribution::getWslPath) ?: throw IllegalStateException()

        languageRuntime.homePath = wslPath
        request.configuration.addLanguageRuntime(languageRuntime)

        val builder = wslParams.toCommandLine(request)
        builder.setWorkingDirectory(wslDistribution.userHome ?: "/") //todo !!! workdir??

        val wslEnvironment = request.prepareEnvironment(TargetProgressIndicator.EMPTY)
        val commandLine = builder.build()
        val process = wslEnvironment.createProcess(commandLine)
        val commandPresentation = commandLine.getCommandPresentation(wslEnvironment)
        return KillableColoredProcessHandler(process, commandPresentation)
    }
}