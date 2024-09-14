package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import org.jdom.Element
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenPlugin
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProject
import java.nio.file.Path

class TychoCompilerPlugin : MavenCompilerFullImportPlugin {
    override fun getGroupId() = "org.eclipse.tycho"

    override fun getArtifactId() = "tycho-compiler-plugin"

    override fun priority() = 20

    override fun getCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): CompilerData {
        return ApacheMavenCompilerPlugin().getCompilerData(project, plugin, localRepositoryPath, contextElementMap)
    }

    override fun getJavaCompilerData(
        project: MavenProject,
        plugin: MavenPlugin,
        compilerData: CompilerData,
        localRepositoryPath: Path,
        contextElementMap: MutableMap<String, Element>
    ): MainJavaCompilerData {
        return MainJavaCompilerData.createDefault()
    }
}