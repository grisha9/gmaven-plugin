package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject

class GroovyMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.gmaven"

    override fun getArtifactId() = "groovy-maven-plugin"

    override fun getContentRoots(mavenProject: MavenProject, plugin: MavenPlugin): PluginContentRoots {
        return GroovyAbstractMavenPlugin.getContentRoots(mavenProject, plugin)
    }
}