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
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.lang.reflect.Field;
import java.util.*;

import static org.apache.maven.plugins.annotations.LifecyclePhase.NONE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

@Mojo(name = "resolve", defaultPhase = NONE, aggregator = true, requiresDependencyResolution = TEST)
public class ResolveProjectMojo extends AbstractMojo {
    private static final String ANNOTATION_PROCESSOR_PATH_MAP = "annotationProcessorPathMap";

    @Component
    private RepositorySystem repositorySystem;
    @Component
    private ArtifactHandlerManager artifactHandlerManager;
    @Component
    private ResolutionErrorHandler resolutionErrorHandler;
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;
    private final Map<String, Field> dependencyCoordinateFieldMap = getDependencyCoordinateFieldMap();

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("!!!-----------------ResolveProjectMojo-----------------------!!!");
        for (MavenProject allProject : session.getAllProjects()) {
            resolveAnnotationProcessor(allProject);
        }
    }

    private void resolveAnnotationProcessor(MavenProject project) throws MojoExecutionException {
        Plugin plugin = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        if (plugin == null || plugin.getConfiguration() == null) return;
        List<DependencyCoordinate> dependencies = getDependencyCoordinates(plugin);
        List<String> paths = resolveProcessorPathEntries(dependencies, project);
        if (paths != null && !paths.isEmpty()) {
            setPathToSession(project, paths);
        }
    }

    private void setPathToSession(MavenProject project, List<String> paths) {
        Map<String, List<String>> processorPathMap = (Map<String, List<String>>) session.getUserProperties()
                .get(ANNOTATION_PROCESSOR_PATH_MAP);
        if (processorPathMap == null) {
            processorPathMap = new HashMap<>();
            session.getUserProperties().put(ANNOTATION_PROCESSOR_PATH_MAP, processorPathMap);
        }
        processorPathMap.put(project.getArtifactId(), paths);
    }

    private List<DependencyCoordinate> getDependencyCoordinates(Plugin plugin) throws MojoExecutionException {
        List<DependencyCoordinate> dependencies = new ArrayList<>();
        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        for (Xpp3Dom dom : configuration.getChildren()) {
            if ("annotationProcessorPaths".equalsIgnoreCase(dom.getName())) {
                getLog().info("!!! annotationProcessorPaths=" + dom);
                for (Xpp3Dom child : dom.getChildren()) {
                    DependencyCoordinate coordinate = getDependencyCoordinate(child);
                    if (coordinate != null) dependencies.add(coordinate);
                }
                return dependencies;
            }
        }
        return dependencies;
    }

    private DependencyCoordinate getDependencyCoordinate(Xpp3Dom dom) throws MojoExecutionException {
        DependencyCoordinate coordinate = new DependencyCoordinate();
        for (Xpp3Dom child : dom.getChildren()) {
            String name = child.getName().toLowerCase();
            String value = child.getValue();
            if (value == null) continue;
            Field field = dependencyCoordinateFieldMap.get(name);
            if (field == null) continue;
            try {
                field.set(coordinate, value);
            } catch (IllegalAccessException e) {
                throw new MojoExecutionException(e.getLocalizedMessage(), e);
            }
        }
        return coordinate.getArtifactId() == null || coordinate.getGroupId() == null || coordinate.getVersion() == null
                ? null : coordinate;
    }

    private Map<String, Field> getDependencyCoordinateFieldMap() {
        Field[] fields = DependencyCoordinate.class.getDeclaredFields();
        Map<String, Field> map = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName().toLowerCase(), field);
        }
        return map;
    }

    private List<String> resolveProcessorPathEntries(
            List<DependencyCoordinate> annotationProcessorPaths, MavenProject project
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