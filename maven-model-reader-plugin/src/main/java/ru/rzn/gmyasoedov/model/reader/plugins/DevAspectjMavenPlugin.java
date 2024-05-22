package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class DevAspectjMavenPlugin implements PluginProcessor {
    @Override
    public String groupId() {
        return "dev.aspectj";
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
