package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
public final class PluginExecution implements Serializable {
    private final String id;
    private final String phase;
    private final List<String> goals;
    private final String configuration;
}
