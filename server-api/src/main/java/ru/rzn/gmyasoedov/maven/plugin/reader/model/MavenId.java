package ru.rzn.gmyasoedov.maven.plugin.reader.model;

public interface MavenId {
    String UNKNOWN_VALUE = "Unknown";

    String getGroupId();

    String getArtifactId();

    String getVersion();
}
