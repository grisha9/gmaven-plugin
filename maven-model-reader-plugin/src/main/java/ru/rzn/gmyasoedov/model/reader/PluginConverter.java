package ru.rzn.gmyasoedov.model.reader;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.*;

public class PluginConverter {
    public static void resolvePluginBody(MavenProject project, Set<String> gPlugins) {
        Model mavenModel = project.getModel();
        if (gPlugins.isEmpty() || mavenModel == null) return;

        Build build = mavenModel.getBuild();
        if (build != null) {
            List<Plugin> plugins = build.getPlugins();
            if (plugins != null) {
                for (Plugin each : plugins) {
                    processPlugin(each, gPlugins, project);
                }
            }
        }
    }

    private static void processPlugin(Plugin each, Set<String> gPlugins, MavenProject project) {
        String pluginKey = each.getGroupId() + ":" + each.getArtifactId();
        if (!gPlugins.contains(pluginKey)) return;
        Map<String, Object> pluginBody = convertPluginBody(each);
        if (!pluginBody.isEmpty()) {
            String key = "gPlugin" + pluginKey;
            project.setContextValue(key, pluginBody);
        }
    }

    public static Map<String, Object> convertPluginBody(Plugin plugin) {
        List<Map<String, Object>> executions = new ArrayList<>(plugin.getExecutions().size());
        for (PluginExecution each : plugin.getExecutions()) {
            executions.add(convertExecution(each));
        }
        Map<String, Object> result = new HashMap<>(5);
        result.put("executions", executions);
        result.put("configuration", convertConfiguration(plugin.getConfiguration()));
        return result;
    }

    private static Map<String, Object> convertExecution(PluginExecution execution) {
        Map<String, Object> result = new HashMap<>(5);
        result.put("id", execution.getId());
        result.put("phase", execution.getPhase());
        result.put("goals", execution.getGoals());
        result.put("configuration", convertConfiguration(execution.getConfiguration()));
        return result;
    }

    private static Map<String, Object> convertConfiguration(Object config) {
        return config instanceof Xpp3Dom ? xppToMap((Xpp3Dom) config) : null;
    }

    private static Map<String, Object> xppToMap(Xpp3Dom xpp) {
        Map<String, Object> result = new HashMap<>();
        Xpp3Dom[] children = xpp.getChildren();
        if (children == null || children.length == 0) {
            result.put(xpp.getName(), xpp.getValue());
        } else {
            HashMap<String, Object> value = new HashMap<>();
            result.put(xpp.getName(), value);
            for (Xpp3Dom each : children) {
                Map<String, Object> child = xppToMap(each);
                value.put(each.getName(), child);
            }
        }
        return result;
    }
}
