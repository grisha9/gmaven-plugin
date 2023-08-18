package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.PluginBody;
import ru.rzn.gmyasoedov.serverapi.model.PluginExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MavenPluginConverter {
    public static MavenPlugin convert(Plugin plugin, MavenProject mavenProject) {
        Object contextValue = mavenProject
                .getContextValue("gPlugin" + plugin.getGroupId() + ":" + plugin.getArtifactId());

        return new MavenPlugin(plugin.getGroupId(),
                plugin.getArtifactId(),
                plugin.getVersion(),
                getPluginBody(contextValue, plugin.getDependencies()));
    }

    private static PluginBody getPluginBody(Object contextValue, List<Dependency> dependencies) {
        if (contextValue instanceof Map) {
            try {
                Map<String, Object> map = (Map<String, Object>) contextValue;
                List<Map<String, Object>> executions = (List<Map<String, Object>>) map.get("executions");
                Object processorPath = map.get("annotationProcessorPath");
                List<String> annotationProcessorPaths = processorPath instanceof List
                        ? (List<String>) processorPath : Collections.<String>emptyList();
                return new PluginBody(mapToExecutions(executions),
                        annotationProcessorPaths,
                        toArtifactList(dependencies),
                        getConfiguration(map));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private static List<PluginExecution> mapToExecutions(List<Map<String, Object>> executions) {
        if (executions == null) Collections.emptyList();
        ArrayList<PluginExecution> result = new ArrayList<>(executions.size());
        for (Map<String, Object> execution : executions) {
            List<String> goals = (List<String>) execution.get("goals");
            result.add(PluginExecution.builder()
                    .id((String) execution.get("id"))
                    .phase((String) execution.get("phase"))
                    .goals(goals == null ? Collections.<String>emptyList() : new ArrayList<>(goals))
                    .configuration(getConfiguration(execution))
                    .build());
        }
        return result;
    }

    private static String getConfiguration(Map<String, Object> map) {
        if (map == null) return null;
        Object configuration = map.get("configuration");
        return configuration instanceof String ? (String) configuration : null;
    }

    private static List<MavenArtifact> toArtifactList(List<Dependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) return null;
        ArrayList<MavenArtifact> result = new ArrayList<>(dependencies.size());
        for (Dependency each : dependencies) {
            result.add(new MavenArtifact(
                    each.getGroupId(),
                    each.getArtifactId(),
                    each.getVersion(),
                    each.getType(),
                    each.getClassifier(),
                    each.getScope(),
                    each.isOptional(),
                    null, false));
        }
        return result;
    }
}
