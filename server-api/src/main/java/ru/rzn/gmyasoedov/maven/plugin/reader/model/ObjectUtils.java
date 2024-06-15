package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import static java.util.Objects.requireNonNull;

public class ObjectUtils {
    private ObjectUtils() {
        throw new IllegalStateException();
    }

    public static <T> T defaultIfNull(T obj, T defaultObj) {
        return (obj != null) ? obj : requireNonNull(defaultObj, "defaultObj");
    }
}
