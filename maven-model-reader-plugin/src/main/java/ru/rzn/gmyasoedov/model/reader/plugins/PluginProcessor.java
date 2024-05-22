package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public interface PluginProcessor {

    String groupId();

    String artifactId();

    void process(MavenProject project, Plugin plugin);
}
