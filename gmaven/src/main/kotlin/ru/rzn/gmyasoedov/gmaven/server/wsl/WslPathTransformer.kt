package ru.rzn.gmyasoedov.gmaven.server.wsl

import com.intellij.execution.wsl.WSLDistribution
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenArtifact
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenMapResult
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProject
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProjectContainer

object WslPathTransformer {
    fun transform(mavenResult: MavenMapResult, wslDistribution: WSLDistribution): MavenMapResult {
        val settings = mavenResult.settings
        settings.settingsPath = wslDistribution.getWindowsPath(settings.settingsPath)
        settings.localRepository = wslDistribution.getWindowsPath(settings.localRepository)
        transform(mavenResult.container, wslDistribution)
        return mavenResult
    }

    private fun transform(container: MavenProjectContainer, wslDistribution: WSLDistribution) {
        val project = container.project
        transformProjectPath(project, wslDistribution)
        for (module in container.modules) {
            transform(module, wslDistribution)
        }
    }

    private fun transformProjectPath(
        project: MavenProject, wslDistribution: WSLDistribution
    ) {
        project.filePath = wslDistribution.getWindowsPath(project.filePath)
        project.basedir = wslDistribution.getWindowsPath(project.basedir)
        if (project.parentFilePath != null) {
            project.parentFilePath = wslDistribution.getWindowsPath(project.parentFilePath)
        }
        if (project.parentArtifact != null && project.parentArtifact.filePath != null) {
            project.parentArtifact.filePath = wslDistribution.getWindowsPath(project.parentArtifact.filePath)
        }
        if (project.buildDirectory != null) {
            project.buildDirectory = wslDistribution.getWindowsPath(project.buildDirectory)
        }
        if (project.outputDirectory != null) {
            project.outputDirectory = wslDistribution.getWindowsPath(project.outputDirectory)
        }
        if (project.testOutputDirectory != null) {
            project.testOutputDirectory = wslDistribution.getWindowsPath(project.testOutputDirectory)
        }
        if (project.generatedPath != null) {
            project.generatedPath = wslDistribution.getWindowsPath(project.generatedPath)
        }
        if (project.testGeneratedPath != null) {
            project.testGeneratedPath = wslDistribution.getWindowsPath(project.testGeneratedPath)
        }
        project.sourceRoots = project.sourceRoots.map { wslDistribution.getWindowsPath(it) }
        project.testSourceRoots = project.testSourceRoots.map { wslDistribution.getWindowsPath(it) }
        project.annotationProcessorPaths = project.annotationProcessorPaths.map { wslDistribution.getWindowsPath(it) }
        project.excludedPaths = project.excludedPaths.map { wslDistribution.getWindowsPath(it) }
        for (each in project.resourceRoots) {
            if (each.directory != null) {
                each.directory = wslDistribution.getWindowsPath(each.directory)
            }
        }
        for (each in project.testResourceRoots) {
            if (each.directory != null) {
                each.directory = wslDistribution.getWindowsPath(each.directory)
            }
        }

        updateArtifacts(project.dependencyArtifacts, wslDistribution)
        updateArtifacts(project.resolvedArtifacts, wslDistribution)
        for (plugin in project.plugins) {
            val pluginDependencies = plugin.body?.dependencies ?: emptyList()
            updateArtifacts(pluginDependencies, wslDistribution)
        }
    }

    private fun updateArtifacts(artifacts: List<MavenArtifact>, wslDistribution: WSLDistribution) {
        for (dependencyArtifact in artifacts) {
            val filePath = dependencyArtifact.filePath
            if (filePath != null) {
                dependencyArtifact.filePath = wslDistribution.getWindowsPath(filePath)
            }
        }
    }
}