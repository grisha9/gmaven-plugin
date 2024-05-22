package ru.rzn.gmyasoedov.model.reader.utils;

import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

public abstract class MavenContextUtils {
    public static final String EXCLUDED_PATHS = "GMaven:excludedPaths";
    public static final String GENERATED_PATH = "GMaven:generatedPath";
    public static final String GENERATED_TEST_PATH = "GMaven:generatedTestPath";

    public static void addStringValue(MavenProject project, String key, String value) {
        project.setContextValue(key, value);
    }

    public static void addListStringValue(MavenProject project, String key, String value) {
        List<String> container = getListContainer(project, key);
        container.add(value);
    }

    private static List<String> getListContainer(MavenProject project, String key) {
        Object contextValue = project.getContextValue(key);
        if (contextValue instanceof ArrayList) {
            return (List<String>) contextValue;
        } else {
            List<String> list = new ArrayList<String>(1);
            project.setContextValue(key, list);
            return list;
        }
    }
}
