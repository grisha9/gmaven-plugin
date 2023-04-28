package ru.rzn.gmyasoedov.serverapi.model;

import java.io.Serializable;
import java.util.Objects;

public class MavenException implements Serializable {
    public final String message;
    public final MavenId artifactId;

    public MavenException(String message, MavenId artifactId) {
        this.message = Objects.requireNonNull(message);
        this.artifactId = Objects.requireNonNull(artifactId);
    }
}
