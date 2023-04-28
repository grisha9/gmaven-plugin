package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public final class PluginExecution implements Serializable {
    private final String id;
    private final String phase;
    private final List<String> goals;
    private final Map<String, Object> configuration;
}
