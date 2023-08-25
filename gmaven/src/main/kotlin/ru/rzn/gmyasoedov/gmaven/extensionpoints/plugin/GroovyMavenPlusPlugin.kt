package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin
import ru.rzn.gmyasoedov.serverapi.model.MavenProject

class GroovyMavenPlusPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.gmavenplus"

    override fun getArtifactId() = "gmavenplus-plugin"

    override fun getContentRoots(
        mavenProject: MavenProject, plugin: MavenPlugin, context: MavenProjectResolver.ProjectResolverContext
    ): PluginContentRoots {
        return GroovyAbstractMavenPlugin.getContentRoots(mavenProject, plugin, context)
    }
}