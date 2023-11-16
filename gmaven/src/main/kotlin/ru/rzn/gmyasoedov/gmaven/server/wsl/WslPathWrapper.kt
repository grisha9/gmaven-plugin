package ru.rzn.gmyasoedov.gmaven.server.wsl

import com.intellij.execution.wsl.WSLDistribution
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import ru.rzn.gmyasoedov.serverapi.model.MavenSettings

object WslPathWrapper {
    fun transformPath(mavenResult: MavenResult, wslDistribution: WSLDistribution?): MavenResult {
        wslDistribution ?: return mavenResult

        val wslMavenResult = MavenResult(
            mavenResult.pluginNotResolved,
            transformSettingsPath(mavenResult.settings, wslDistribution),
            mavenResult.projectContainer,
            mavenResult.exceptions
        )
        val container = wslMavenResult.projectContainer ?: return wslMavenResult
        transformPath(container, wslDistribution)
        return wslMavenResult
    }

    private fun transformSettingsPath(settings: MavenSettings, wslDistribution: WSLDistribution): MavenSettings {
        return MavenSettings(
            settings.modulesCount,
            wslDistribution.getWindowsPath(settings.localRepository),
            wslDistribution.getWindowsPath(settings.settingsPath),
            settings.profiles,
            settings.remoteRepositories
        )
    }

    private fun transformPath(container: MavenProjectContainer, wslDistribution: WSLDistribution) {
        val project = container.project
        transformProjectPath(project, wslDistribution)
        for (module in container.modules) {
            transformPath(module, wslDistribution)
        }
    }

    fun transformProjectPath(projects: List<MavenProject>, wslDistribution: WSLDistribution?) {
        wslDistribution ?: return
        projects.forEach { transformProjectPath(it, wslDistribution) }
    }

    private fun transformProjectPath(
        project: MavenProject, wslDistribution: WSLDistribution
    ) {
        project.filePath = wslDistribution.getWindowsPath(project.filePath)
        project.basedir = wslDistribution.getWindowsPath(project.basedir)
        if (project.parentFilePath != null) {
            project.parentFilePath = wslDistribution.getWindowsPath(project.parentFilePath)
        }
        for (dependencyArtifact in project.dependencyArtifacts) {
            val filePath = dependencyArtifact.filePath
            if (filePath != null) {
                dependencyArtifact.filePath = wslDistribution.getWindowsPath(filePath)
            }
        }
    }
}