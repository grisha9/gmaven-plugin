package ru.rzn.gmyasoedov.model.reader;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
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
import static ru.rzn.gmyasoedov.model.reader.PluginConverter.resolvePluginBody;

@Mojo(name = "resolve", defaultPhase = NONE, aggregator = true, requiresDependencyResolution = TEST)
public class ResolveProjectMojo extends AbstractMojo {
    private static final String ANNOTATION_PROCESSOR_PATH = "annotationProcessorPath";

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
        Set<String> gPluginSet = getPluginForBodyProcessing();
        getLog().info("!!!-----------------ResolveProjectMojo-----------------------!!!");
        for (MavenProject mavenProject : session.getAllProjects()) {
            resolveAnnotationProcessor(mavenProject);
            resolvePluginBody(mavenProject, gPluginSet);
        }
    }

    private Set<String> getPluginForBodyProcessing() {
        String gPlugins = System.getProperty("gmaveng.plugins", "");
        if(gPlugins.isEmpty()) return Collections.emptySet();
        String[] gPluginsArray = gPlugins.split(";");
        HashSet<String> gPluginSet = new HashSet<>(gPluginsArray.length);
        Collections.addAll(gPluginSet, gPluginsArray);
        return gPluginSet;
    }

    private void resolveAnnotationProcessor(MavenProject project) throws MojoExecutionException {
        Plugin plugin = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        if (plugin == null || plugin.getConfiguration() == null) return;
        List<DependencyCoordinate> dependencies = getDependencyCoordinates(plugin);
        List<String> paths = GUtils.resolveArtifacts(
                dependencies, project,
                repositorySystem, artifactHandlerManager,
                resolutionErrorHandler, session
        );
        if (paths != null && !paths.isEmpty()) {
            setPathToSession(project, paths);
        }
    }

    private void setPathToSession(MavenProject project, List<String> paths) {
        project.setContextValue(ANNOTATION_PROCESSOR_PATH, paths);
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
}