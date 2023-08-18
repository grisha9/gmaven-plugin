package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.List;

@Getter
public final class PluginBody implements Serializable {
    @NonNull
    private final List<PluginExecution> executions;
    @NonNull
    private final List<String> annotationProcessorPaths;
    private final List<MavenArtifact> dependencies;
    private final String configuration;

    public PluginBody(@NonNull List<PluginExecution> executions,
                      @NonNull List<String> annotationProcessorPaths,
                      List<MavenArtifact> dependencies,
                      String configuration) {
        this.executions = executions;
        this.annotationProcessorPaths = annotationProcessorPaths;
        this.dependencies = dependencies;
        this.configuration = configuration;
    }
}
