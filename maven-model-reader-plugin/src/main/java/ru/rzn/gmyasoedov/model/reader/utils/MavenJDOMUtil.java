package ru.rzn.gmyasoedov.model.reader.utils;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MavenJDOMUtil {

    public static Xpp3Dom findChildByPath(Xpp3Dom element, String path) {
        int i = 0;
        while (element != null) {
            int dot = path.indexOf('.', i);
            if (dot == -1) {
                return element.getChild(path.substring(i));
            }

            element = element.getChild(path.substring(i, dot));
            i = dot + 1;
        }

        return null;
    }

    public static String findChildValueByPath(Xpp3Dom element, String path, String defaultValue) {
        Xpp3Dom child = findChildByPath(element, path);
        if (child == null) return defaultValue;
        String childValue = child.getValue();
        if (childValue == null) return defaultValue;
        return childValue.isEmpty() ? defaultValue : childValue;
    }

    public static String findChildValueByPath(Xpp3Dom element, String path) {
        return findChildValueByPath(element, path, null);
    }

    public static boolean hasChildByPath(Xpp3Dom element, String path) {
        return findChildByPath(element, path) != null;
    }

    public static List<Xpp3Dom> findChildrenByPath(Xpp3Dom element, String path, String subPath) {
        return collectChildren(findChildByPath(element, path), subPath);
    }

    public static List<String> findChildrenValuesByPath(Xpp3Dom element, String path, String childrenName) {
        List<String> result = new ArrayList<>();
        for (Xpp3Dom each : findChildrenByPath(element, path, childrenName)) {
            String value = each.getValue();
            if (value != null && !value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private static List<Xpp3Dom> collectChildren(Xpp3Dom container, String subPath) {
        if (container == null) return Collections.emptyList();

        int firstDot = subPath.indexOf('.');

        if (firstDot == -1) {
            Xpp3Dom[] children = container.getChildren(subPath);
            return (children == null || children.length == 0)
                    ? Collections.<Xpp3Dom>emptyList() : Arrays.asList(children);
        }

        String childName = subPath.substring(0, firstDot);
        String pathInChild = subPath.substring(firstDot + 1);

        List<Xpp3Dom> result = new ArrayList<>();

        for (Xpp3Dom each : container.getChildren(childName)) {
            Xpp3Dom child = findChildByPath(each, pathInChild);
            if (child != null) result.add(child);
        }
        return result;
    }
}
