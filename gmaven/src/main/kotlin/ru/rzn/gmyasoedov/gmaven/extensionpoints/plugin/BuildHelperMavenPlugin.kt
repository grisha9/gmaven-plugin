package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

class BuildHelperMavenPlugin : MavenFullImportPlugin {
    override fun getGroupId() = "org.codehaus.mojo"

    override fun getArtifactId() = "build-helper-maven-plugin"
}