package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class CodehausAspectjMavenPlugin implements PluginProcessor {
    @Override
    public String groupId() {
        return "org.codehaus.mojo";
    }

    @Override
    public String artifactId() {
        return "aspectj-maven-plugin";
    }

    @Override
    public void process(MavenProject project, Plugin plugin) {
        AbstractAspectJPluginProcessor.process(project, plugin);
    }
}
