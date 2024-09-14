package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.event.handler.model.MavenPluginWrapper;
import ru.rzn.gmyasoedov.event.handler.model.PluginBodyWrapper;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenArtifact;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenPlugin;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.PluginBody;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.PluginExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MavenPluginConverter {
    public static MavenPlugin convert(MavenPluginWrapper plugin, MavenProject mavenProject) {
        MavenPlugin result = new MavenPlugin();
        result.setGroupId(plugin.getGroupId());
        result.setArtifactId(plugin.getArtifactId());
        result.setVersion(plugin.getVersion());
        if (plugin.getBody() != null) {
            PluginBody body = new PluginBody();
            body.setConfiguration(plugin.getBody().getConfiguration());
            body.setDependencies(plugin.getBody().getDependencies());
            body.setExecutions(plugin.getBody().getExecutions());
            result.setBody(body);
        }
        return result;
    }

    public static MavenPluginWrapper convert(Plugin plugin, MavenProject mavenProject) {
        Object contextValue = mavenProject
                .getContextValue("gPlugin" + plugin.getGroupId() + ":" + plugin.getArtifactId());

        return new MavenPluginWrapper(plugin.getGroupId(),
                plugin.getArtifactId(),
                plugin.getVersion(),
                getPluginBody(contextValue, plugin.getDependencies()));
    }

    private static PluginBodyWrapper getPluginBody(Object contextValue, List<Dependency> dependencies) {
        if (contextValue instanceof Map) {
            try {
                Map<String, Object> map = (Map<String, Object>) contextValue;
                List<Map<String, Object>> executions = (List<Map<String, Object>>) map.get("executions");
                Object processorPath = map.get("annotationProcessorPath");
                List<String> annotationProcessorPaths = processorPath instanceof List
                        ? (List<String>) processorPath : Collections.<String>emptyList();
                return new PluginBodyWrapper(mapToExecutions(executions),
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
        if (executions == null) return Collections.emptyList();
        ArrayList<PluginExecution> result = new ArrayList<>(executions.size());
        for (Map<String, Object> execution : executions) {
            List<String> goals = (List<String>) execution.get("goals");
            PluginExecution pluginExecution = new PluginExecution();
            pluginExecution.setId((String) execution.get("id"));
            pluginExecution.setPhase((String) execution.get("phase"));
            pluginExecution.setGoals(goals == null ? Collections.<String>emptyList() : new ArrayList<>(goals));
            pluginExecution.setConfiguration(getConfiguration(execution));
            result.add(pluginExecution);
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
            MavenArtifact artifact = new MavenArtifact();
            artifact.setGroupId(each.getGroupId());
            artifact.setArtifactId(each.getArtifactId());
            artifact.setVersion(each.getVersion());
            artifact.setType(each.getType());
            artifact.setClassifier(each.getClassifier());
            artifact.setScope(each.getScope());
            artifact.setOptional(each.isOptional());
            artifact.setResolved(false);
            result.add(artifact);
        }
        return result;
    }
}
