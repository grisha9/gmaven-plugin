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
        if (config instanceof Xpp3Dom) {
            HashMap<String, Object> result = new HashMap<>();
            xppToMap((Xpp3Dom) config, result);
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    private static void xppToMap(Xpp3Dom xpp, Map<String, Object> result) {
        Xpp3Dom[] children = xpp.getChildren();
        if (children == null || children.length == 0) {
            result.put(xpp.getName(), xpp.getValue());
        } else {
            Map<String, Object> node = new HashMap<>();
            Object value = result.get(xpp.getName());
            if (value != null) {
                if (value instanceof List) {
                    ((List) value).add(node);
                } else {
                    ArrayList<Object> objectList = new ArrayList<>(3);
                    objectList.add(value);
                    objectList.add(node);
                    result.put(xpp.getName(), objectList);
                }
            } else {
                result.put(xpp.getName(), node);
            }

            for (Xpp3Dom each : children) {
                xppToMap(each, node);
            }
        }
    }
}
