package ru.rzn.gmyasoedov.model.reader;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import ru.rzn.gmyasoedov.model.reader.plugins.PluginProcessorManager;
import ru.rzn.gmyasoedov.model.reader.utils.GUtils;

import java.lang.reflect.Field;
import java.util.*;

import static java.lang.String.format;
import static org.apache.maven.plugins.annotations.LifecyclePhase.NONE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

@Mojo(name = "resolve", defaultPhase = NONE, aggregator = true, requiresDependencyResolution = TEST, threadSafe = true)
public class ResolveProjectMojo extends AbstractMojo {
    private static final String ANNOTATION_PROCESSOR_PATH = "annotationProcessorPath";
    private static final String GMAVEN_PLUGINS = "gmaven.plugins";
    private static final String GMAVEN_PLUGIN_ANNOTATION_PROCESSOR = "gmaven.plugin.annotation.paths.%s";

    @Component
    private RepositorySystem repositorySystem;
    @Component
    private ArtifactHandlerManager artifactHandlerManager;
    @Component
    private ResolutionErrorHandler resolutionErrorHandler;
    @Component
    private MavenSession session;
    @Parameter(property = "gmaven.resolvedArtifactIds")
    protected Set<String> resolvedArtifactIds;

    private final Map<String, Field> dependencyCoordinateFieldMap = getDependencyCoordinateFieldMap();
    private List<ArtifactResolutionException> resolveArtifactErrors;

    @Override
    public void execute() throws MojoExecutionException {
        if (!getResolvedArtifactIds().isEmpty()) {
            getLog().info("resolvedArtifactIds " + resolvedArtifactIds);
        }
        resolveArtifactErrors = new ArrayList<>();
        Set<String> gPluginSet = getPluginForBodyProcessing();
        getLog().info("ResolveProjectMojo: " + gPluginSet);
        if (session.getAllProjects() == null) return;
        for (MavenProject mavenProject : session.getAllProjects()) {
            resolvePluginBody(mavenProject, gPluginSet);
        }
        for (ArtifactResolutionException error : resolveArtifactErrors) {
            getLog().debug("Resolution of annotationProcessorPath dependencies failed: "
                    + error.getLocalizedMessage(), error);
        }
    }

    public void resolvePluginBody(MavenProject project, Set<String> gPlugins) throws MojoExecutionException {
        Model mavenModel = project.getModel();
        if (gPlugins.isEmpty() || mavenModel == null) return;

        Build build = mavenModel.getBuild();
        if (build != null) {
            List<Plugin> plugins = build.getPlugins();
            if (plugins != null) {
                for (Plugin each : plugins) {
                    processPlugin(each, gPlugins, project);
                }
            }
        }
    }

    private void processPlugin(Plugin each, Set<String> gPlugins, MavenProject project)
            throws MojoExecutionException {
        String pluginKey = each.getGroupId() + ":" + each.getArtifactId();
        if (!gPlugins.contains(pluginKey)) return;
        PluginProcessorManager.process(project, each);
        Map<String, Object> pluginBody = convertPluginBody(project, each);
        if (!pluginBody.isEmpty()) {
            String key = "gPlugin" + pluginKey;
            project.setContextValue(key, pluginBody);
        }
        if (getResolvedArtifactIds().contains(each.getArtifactId())) {
            resolve(each.getArtifactId(), each.getGroupId(), each.getVersion(), project);
            List<Dependency> dependencies = each.getDependencies();
            if (dependencies == null) return;
            for (Dependency dependency : dependencies) {
                resolve(dependency.getArtifactId(), dependency.getGroupId(), dependency.getVersion(), project);
            }
        }
    }

    private void resolve(String artifactId, String groupId, String version, MavenProject project)
            throws MojoExecutionException {
        DependencyCoordinate coordinateDep = new DependencyCoordinate();
        coordinateDep.setArtifactId(artifactId);
        coordinateDep.setGroupId(groupId);
        coordinateDep.setVersion(version);
        getLog().info("gmaven.resolvedArtifactId " + coordinateDep);
        GUtils.resolveArtifacts(
                Collections.singletonList(coordinateDep), project,
                repositorySystem, artifactHandlerManager, resolutionErrorHandler, session,
                new ArrayList<ArtifactResolutionException>()
        );
    }

    private Map<String, Object> convertPluginBody(MavenProject project, Plugin plugin)
            throws MojoExecutionException {
        String annotationProcessorPaths = getPluginAnnotationProcessorPaths(plugin);
        List<String> resolvedPaths = resolveAnnotationProcessor(project, plugin, annotationProcessorPaths);
        List<Map<String, Object>> executions = new ArrayList<>(plugin.getExecutions().size());
        for (PluginExecution each : plugin.getExecutions()) {
            executions.add(convertExecution(each));
        }
        Map<String, Object> result = new HashMap<>(5);
        result.put("executions", executions);
        result.put("configuration", convertConfiguration(plugin.getConfiguration()));
        result.put(ANNOTATION_PROCESSOR_PATH, resolvedPaths);
        return result;
    }

    private static Map<String, Object> convertExecution(PluginExecution execution) {
        Map<String, Object> result = new HashMap<>(5);
        result.put("id", execution.getId());
        result.put("phase", execution.getPhase());
        result.put("goals", execution.getGoals());
        result.put("configuration", convertConfiguration(execution.getConfiguration()));
        return result;
    }

    private static String convertConfiguration(Object config) {
        if (config instanceof Xpp3Dom) {
            return config.toString();
        } else {
            return null;
        }
    }

    private Set<String> getPluginForBodyProcessing() {
        String gPlugins = System.getProperty(GMAVEN_PLUGINS, "");
        if (gPlugins.isEmpty()) return Collections.emptySet();
        String[] gPluginsArray = gPlugins.split(";");
        HashSet<String> gPluginSet = new HashSet<>(gPluginsArray.length * 2);
        Collections.addAll(gPluginSet, gPluginsArray);
        return gPluginSet;
    }

    private static String getPluginAnnotationProcessorPaths(Plugin plugin) {
        String path = System.getProperty(format(GMAVEN_PLUGIN_ANNOTATION_PROCESSOR, plugin.getArtifactId()), "");
        return path.isEmpty() ? null : path;
    }

    private List<String> resolveAnnotationProcessor(MavenProject project,
                                                    Plugin plugin,
                                                    String annotationProcessorPaths)
            throws MojoExecutionException {
        if (annotationProcessorPaths == null || plugin == null || plugin.getConfiguration() == null) return null;
        List<DependencyCoordinate> dependencies = getDependencyCoordinates(plugin, annotationProcessorPaths);
        getLog().debug("Dependencies for resolve " + dependencies);
        List<String> paths = GUtils.resolveArtifacts(
                dependencies, project,
                repositorySystem, artifactHandlerManager,
                resolutionErrorHandler, session,
                resolveArtifactErrors
        );
        if (paths != null && !paths.isEmpty()) {
            getLog().info("annotation processor paths " + paths);
        }
        return paths;
    }

    private List<DependencyCoordinate> getDependencyCoordinates(Plugin plugin, String annotationProcessorPaths)
            throws MojoExecutionException {
        List<DependencyCoordinate> dependencies = new ArrayList<>();
        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        for (Xpp3Dom dom : configuration.getChildren()) {
            if (annotationProcessorPaths.equalsIgnoreCase(dom.getName())) {
                getLog().debug("!!! annotationProcessorPaths=" + dom);
                for (Xpp3Dom child : dom.getChildren()) {
                    DependencyCoordinate coordinate = getDependencyCoordinate(child);
                    if (coordinate != null) {
                        dependencies.add(coordinate);
                    }
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

    private Set<String> getResolvedArtifactIds() {
        return resolvedArtifactIds != null ? resolvedArtifactIds : Collections.<String>emptySet();
    }
}