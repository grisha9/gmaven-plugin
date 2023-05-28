package ru.rzn.gmyasoedov.event.handler;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.graph.DependencyNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventSpyResultHolder {
    public Map<String, List<DependencyNode>> dependencyResult = new HashMap<>();
    public MavenSession session;
    public MavenExecutionResult executionResult;
    public List<String> settingsActiveProfiles;
}
