package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class GroovyMavenPlugin implements PluginProcessor{
    @Override
    public String groupId() {
        return "org.codehaus.gmaven";
    }

    @Override
    public String artifactId() {
        return "groovy-maven-plugin";
    }

    @Override
    public void process(MavenProject project, Plugin plugin) {
        AbstractGroovyPluginProcessor.process(project, plugin);
    }
}
