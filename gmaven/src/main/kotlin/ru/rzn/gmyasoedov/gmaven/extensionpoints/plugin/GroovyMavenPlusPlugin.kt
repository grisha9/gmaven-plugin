package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

class GroovyMavenPlusPlugin : GroovyAbstractMavenPlugin() {
    override fun getGroupId() = "org.codehaus.gmavenplus"

    override fun getArtifactId() = "gmavenplus-plugin"
}