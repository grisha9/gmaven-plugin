package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class GroovyMavenPlusPlugin implements PluginProcessor{
    @Override
    public String groupId() {
        return "org.codehaus.gmavenplus";
    }

    @Override
    public String artifactId() {
        return "gmavenplus-plugin";
    }

    @Override
    public void process(MavenProject project, Plugin plugin) {
        AbstractGroovyPluginProcessor.process(project, plugin);
    }
}
