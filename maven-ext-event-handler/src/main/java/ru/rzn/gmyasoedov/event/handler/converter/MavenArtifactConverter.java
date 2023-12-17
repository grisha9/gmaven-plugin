package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact;

public class MavenArtifactConverter {
    public static MavenArtifact convert(Artifact artifact) {
        return new MavenArtifact(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getBaseVersion() != null ? artifact.getBaseVersion() : artifact.getVersion(),
                artifact.getType(),
                artifact.getClassifier(),
                artifact.getScope(),
                artifact.isOptional(),
                artifact.getFile(),
                artifact.isResolved());
    }

    public static MavenArtifact convert(MavenProject project) {
        return new MavenArtifact(project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                project.getPackaging(),
                null,
                null,
                false,
                project.getFile(),true);
    }
}
