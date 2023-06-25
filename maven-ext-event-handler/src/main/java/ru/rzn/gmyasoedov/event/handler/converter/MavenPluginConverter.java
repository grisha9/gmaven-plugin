package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
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
                getPluginBody(contextValue));
    }

    private static PluginBody getPluginBody(Object contextValue) {
        if (contextValue instanceof Map) {
            try {
                Map<String, Object> map = (Map<String, Object>) contextValue;
                List<Map<String, Object>> executions = (List<Map<String, Object>>) map.get("executions");
                Object processorPath = map.get("annotationProcessorPath");
                List<String> annotationProcessorPaths = processorPath instanceof List
                        ? (List<String>) processorPath : Collections.<String>emptyList();
                return PluginBody.builder()
                        .configuration(getConfiguration(map))
                        .executions(mapToExecutions(executions))
                        .annotationProcessorPaths(annotationProcessorPaths)
                        .build();
            } catch (Exception e) {
                System.err.println(e);
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
}
