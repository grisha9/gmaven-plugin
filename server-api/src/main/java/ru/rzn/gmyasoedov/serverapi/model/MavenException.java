package ru.rzn.gmyasoedov.serverapi.model;

import java.io.Serializable;

public class MavenException implements Serializable {
    public final String message;
    public final MavenId artifactId;
    public final String projectFilePath;

    public MavenException(String message, MavenId artifactId, String projectFilePath) {
        this.message = message;
        this.artifactId = artifactId;
        this.projectFilePath = projectFilePath;
    }
}
