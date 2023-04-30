package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.pom.java.LanguageLevel.HIGHEST
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.CompilerData
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MavenWrapperDistribution
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import java.nio.file.Path
import java.util.*

fun getMavenHome(distributionSettings: DistributionSettings): Path {
    if (distributionSettings.path != null) return distributionSettings.path
    if (distributionSettings.url != null) {
        val mavenHome = MavenWrapperDistribution.getOrDownload(distributionSettings.url)
        return mavenHome.path
    }
    throw ExternalSystemException("maven home is empty");
}

fun getCompilerData(mavenProject: MavenProject, mavenResult: MavenResult): CompilerData {
    val localRepoPath = mavenResult.localRepository ?: return CompilerData(HIGHEST, Collections.emptyList());
    val compilerPlugin = MavenFullImportPlugin.EP_NAME.extensionList
        .filterIsInstance<MavenCompilerFullImportPlugin>()
        .firstOrNull() ?: return CompilerData(HIGHEST, Collections.emptyList())

    for (plugin in mavenProject.plugins) {
        if (compilerPlugin.isApplicable(plugin)) {
            return compilerPlugin.getCompilerData(mavenProject, plugin, Path.of(localRepoPath))
        }
    }
    return CompilerData(HIGHEST, Collections.emptyList());
}