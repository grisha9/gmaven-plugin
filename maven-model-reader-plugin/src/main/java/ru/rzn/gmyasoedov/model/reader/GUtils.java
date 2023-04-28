package ru.rzn.gmyasoedov.model.reader;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class GUtils {

    public static List<String> resolveArtifacts(
            List<DependencyCoordinate> annotationProcessorPaths,
            MavenProject project,
            RepositorySystem repositorySystem,
            ArtifactHandlerManager artifactHandlerManager,
            ResolutionErrorHandler resolutionErrorHandler,
            MavenSession session
    ) throws MojoExecutionException {
        if (annotationProcessorPaths == null || annotationProcessorPaths.isEmpty()) {
            return null;
        }

        try {
            Set<String> elements = new LinkedHashSet<>();
            for (DependencyCoordinate coord : annotationProcessorPaths) {
                ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(coord.getType());

                Artifact artifact = new DefaultArtifact(
                        coord.getGroupId(),
                        coord.getArtifactId(),
                        VersionRange.createFromVersionSpec(coord.getVersion()),
                        Artifact.SCOPE_RUNTIME,
                        coord.getType(),
                        coord.getClassifier(),
                        handler,
                        false);

                ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                        .setArtifact(artifact)
                        .setResolveRoot(true)
                        .setResolveTransitively(true)
                        .setLocalRepository(session.getLocalRepository())
                        .setRemoteRepositories(project.getRemoteArtifactRepositories());

                ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);

                resolutionErrorHandler.throwErrors(request, resolutionResult);

                for (Artifact resolved : resolutionResult.getArtifacts()) {
                    elements.add(resolved.getFile().getAbsolutePath());
                }
            }
            return new ArrayList<>(elements);
        } catch (Exception e) {
            throw new MojoExecutionException("Resolution of annotationProcessorPath dependencies failed: "
                    + e.getLocalizedMessage(), e);
        }
    }
}
