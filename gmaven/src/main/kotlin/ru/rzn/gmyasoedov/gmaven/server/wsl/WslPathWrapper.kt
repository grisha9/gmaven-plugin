package ru.rzn.gmyasoedov.gmaven.server.wsl

import com.intellij.execution.wsl.WSLDistribution
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer
import ru.rzn.gmyasoedov.serverapi.model.MavenResult

object WslPathWrapper {
    fun transformPath(mavenResult: MavenResult, wslDistribution: WSLDistribution?) {
        wslDistribution ?: return
        val container = mavenResult.projectContainer ?: return
        transformPath(container, wslDistribution)
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