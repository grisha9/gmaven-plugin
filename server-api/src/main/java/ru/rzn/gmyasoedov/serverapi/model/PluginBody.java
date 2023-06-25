package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
public final class PluginBody implements Serializable {
    @NonNull
    private final List<PluginExecution> executions;
    @NonNull
    private final List<String> annotationProcessorPaths;
    private final String configuration;
}
