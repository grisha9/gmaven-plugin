package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

class GroovyMavenPlugin : GroovyAbstractMavenPlugin() {
    override fun getGroupId() = "org.codehaus.gmaven"

    override fun getArtifactId() = "groovy-maven-plugin"
}