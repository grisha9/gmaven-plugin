package ru.rzn.gmyasoedov.gmaven.utils;

import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MavenJDOMUtil {

    @Nullable
    public static Element findChildByPath(@Nullable Element element, String path) {
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

    public static String findChildValueByPath(@Nullable Element element, String path, String defaultValue) {
        Element child = findChildByPath(element, path);
        if (child == null) return defaultValue;
        String childValue = child.getTextTrim();
        return childValue.isEmpty() ? defaultValue : childValue;
    }

    public static String findChildValueByPath(@Nullable Element element, String path) {
        return findChildValueByPath(element, path, null);
    }

    public static boolean hasChildByPath(@Nullable Element element, String path) {
        return findChildByPath(element, path) != null;
    }

    public static List<Element> findChildrenByPath(@Nullable Element element, String path, String subPath) {
        return collectChildren(findChildByPath(element, path), subPath);
    }

    public static List<String> findChildrenValuesByPath(@Nullable Element element, String path, String childrenName) {
        List<String> result = new ArrayList<>();
        for (Element each : findChildrenByPath(element, path, childrenName)) {
            String value = each.getTextTrim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private static List<Element> collectChildren(@Nullable Element container, String subPath) {
        if (container == null) return Collections.emptyList();

        int firstDot = subPath.indexOf('.');

        if (firstDot == -1) {
            return container.getChildren(subPath);
        }

        String childName = subPath.substring(0, firstDot);
        String pathInChild = subPath.substring(firstDot + 1);

        List<Element> result = new ArrayList<>();

        for (Element each : container.getChildren(childName)) {
            Element child = findChildByPath(each, pathInChild);
            if (child != null) result.add(child);
        }
        return result;
    }
}
