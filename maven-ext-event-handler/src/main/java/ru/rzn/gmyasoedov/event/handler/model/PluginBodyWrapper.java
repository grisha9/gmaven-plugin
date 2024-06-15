package ru.rzn.gmyasoedov.event.handler.model;

import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenArtifact;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.PluginExecution;

import java.io.Serializable;
import java.util.List;

public final class PluginBodyWrapper implements Serializable {
    private final List<PluginExecution> executions;
    private final List<String> annotationProcessorPaths;
    private final List<MavenArtifact> dependencies;
    private final String configuration;

    public PluginBodyWrapper(List<PluginExecution> executions,
                             List<String> annotationProcessorPaths,
                             List<MavenArtifact> dependencies,
                             String configuration) {
        this.executions = executions;
        this.annotationProcessorPaths = annotationProcessorPaths;
        this.dependencies = dependencies;
        this.configuration = configuration;
    }

    public List<PluginExecution> getExecutions() {
        return executions;
    }

    public List<String> getAnnotationProcessorPaths() {
        return annotationProcessorPaths;
    }

    public List<MavenArtifact> getDependencies() {
        return dependencies;
    }

    public String getConfiguration() {
        return configuration;
    }
}
