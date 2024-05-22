package ru.rzn.gmyasoedov.model.reader.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import ru.rzn.gmyasoedov.model.reader.DependencyCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Objects.requireNonNull;

public abstract class GUtils {

    public static List<String> resolveArtifacts(
            List<DependencyCoordinate> dependencyCoordinates,
            MavenProject project,
            RepositorySystem repositorySystem,
            ArtifactHandlerManager artifactHandlerManager,
            ResolutionErrorHandler resolutionErrorHandler,
            MavenSession session,
            List<ArtifactResolutionException> resolveArtifactErrors
    ) throws MojoExecutionException {
        if (dependencyCoordinates == null || dependencyCoordinates.isEmpty()) {
            return null;
        }

        try {
            Set<String> elements = new LinkedHashSet<>();
            for (DependencyCoordinate coord : dependencyCoordinates) {
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
        } catch (ArtifactResolutionException e) {
            File file = tryGetLocalArtifactFromTarget(project);
            if (file != null) {
                return Collections.singletonList(file.getAbsolutePath());
            }
            resolveArtifactErrors.add(e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new MojoExecutionException("Resolution of annotationProcessorPath dependencies failed: "
                    + e.getLocalizedMessage(), e);
        }
    }

    private static File tryGetLocalArtifactFromTarget(MavenProject project) {
        try {
            File[] files = new File(project.getBuild().getDirectory()).listFiles();
            if (files == null) return null;
            List<File> results = new ArrayList<>(2);
            for (File file : files) {
                if (!file.isDirectory() && file.getName().endsWith(".jar")) {
                    results.add(file);
                }
            }
            if (results.size() == 1) return results.get(0);
            for (File result : results) {
                if (result.getName().contains(project.getArtifactId())) {
                    return result;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getAbsolutePath(String mavenPath, MavenProject mavenProject) {
        if (mavenPath == null) return null;
        try {
            Path path = Paths.get(mavenPath);
            if (path.isAbsolute()) {
                return mavenPath;
            } else {
                return Paths.get(mavenProject.getBasedir().getAbsolutePath(), mavenPath).toAbsolutePath().toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String getAbsolutePath(String mavenPath, String baseAbsolutePath) {
        if (mavenPath == null) return null;
        try {
            Path path = Paths.get(mavenPath);
            if (path.isAbsolute()) {
                return mavenPath;
            } else {
                return Paths.get(baseAbsolutePath, mavenPath).toAbsolutePath().toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T defaultIfNull(T obj, T defaultObj) {
        return (obj != null) ? obj : requireNonNull(defaultObj, "defaultObj");
    }
}
