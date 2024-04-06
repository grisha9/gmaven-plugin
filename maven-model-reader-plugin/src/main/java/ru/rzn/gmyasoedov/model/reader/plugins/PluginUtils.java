package ru.rzn.gmyasoedov.model.reader.plugins;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PluginUtils {

    public static List<String> getPathList(Object config, String parameterName) {
        Xpp3Dom sourceDirs = config instanceof Xpp3Dom ? ((Xpp3Dom) config).getChild(parameterName) : null;
        if (sourceDirs == null) return Collections.emptyList();
        Xpp3Dom[] pathElements = sourceDirs.getChildren();
        ArrayList<String> result = new ArrayList<>(1);
        for (Xpp3Dom sourceDirElement : pathElements) {
            String value = sourceDirElement.getValue();
            if (value == null) continue;
            result.add(value);
        }
        return result;
    }
}
