package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

class GroovyMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.gmaven"

    override fun getArtifactId() = "groovy-maven-plugin"
}