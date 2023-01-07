package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.artifact.Artifact;
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact;

public class MavenArtifactConverter {
    public static MavenArtifact convert(Artifact artifact) {
        return new MavenArtifact(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getType(),
                artifact.getClassifier(),
                artifact.getScope(),
                artifact.isOptional(),
                artifact.getFile(),
                artifact.isResolved());
    }
}
