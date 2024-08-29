package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.eclipse.aether.graph.DependencyNode;
import ru.rzn.gmyasoedov.event.handler.model.MavenPluginWrapper;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static ru.rzn.gmyasoedov.serverapi.GServerUtils.getFilePath;

public class MavenProjectConverter {

    public static MavenProject convert(org.apache.maven.project.MavenProject mavenProject,
                                       Map<String, List<DependencyNode>> dependencyResultMap,
                                       boolean readOnly) {
        if (dependencyResultMap == null) {
        }

        List<MavenPluginWrapper> pluginWrappers = new ArrayList<>(mavenProject.getBuildPlugins().size());
        for (Plugin each : mavenProject.getBuildPlugins()) {
            pluginWrappers.add(MavenPluginConverter.convert(each, mavenProject));
        }
        List<MavenPlugin> plugins = new ArrayList<>(pluginWrappers.size());
        for (MavenPluginWrapper each : pluginWrappers) {
            plugins.add(MavenPluginConverter.convert(each, mavenProject));
        }

        List<MavenArtifact> artifacts = new ArrayList<>(mavenProject.getArtifacts().size());

        Map<Artifact, MavenArtifact> convertedArtifactMap = new HashMap<>(mavenProject.getArtifacts().size());
        for (Artifact artifact : mavenProject.getArtifacts()) {
            MavenArtifact mavenArtifact = MavenArtifactConverter.convert(artifact);
            artifacts.add(mavenArtifact);
            convertedArtifactMap.put(artifact, mavenArtifact);
        }
        if (readOnly) {
            addReferencedProjects(mavenProject, artifacts, 0);
        }

        List<String> modulesDir = convertModules(mavenProject.getBasedir(), mavenProject.getModules());

        MavenProject result = new MavenProject();
        result.setGroupId(mavenProject.getGroupId());
        result.setArtifactId(mavenProject.getArtifactId());
        result.setVersion(mavenProject.getVersion());
        result.setPackaging(mavenProject.getPackaging());
        result.setName(mavenProject.getName());
        result.setBasedir(mavenProject.getBasedir().getAbsolutePath());
        result.setFilePath(getFilePath(mavenProject.getFile()));
        result.setParentFilePath(getFilePath(mavenProject.getParentFile()));
        result.setModulesDir(modulesDir);
        result.setPlugins(plugins);
        result.setSourceRoots(mavenProject.getCompileSourceRoots());
        result.setTestSourceRoots(mavenProject.getTestCompileSourceRoots());
        result.setResourceRoots(convertResource(mavenProject.getResources()));
        result.setTestResourceRoots(convertResource(mavenProject.getTestResources()));
        result.setBuildDirectory(mavenProject.getBuild().getDirectory());
        result.setOutputDirectory(mavenProject.getBuild().getOutputDirectory());
        result.setTestOutputDirectory(mavenProject.getBuild().getTestOutputDirectory());
        result.setResolvedArtifacts(artifacts);
        result.setParentArtifact(mavenProject.getParent() != null
                ? MavenArtifactConverter.convert(mavenProject.getParent()) : null);
        result.setProperties(getProperties(mavenProject));
        result.setRemoteRepositories(Collections.<MavenRemoteRepository>emptyList());
        //.remoteRepositories(RemoteRepositoryConverter.convert(mavenProject.getRemoteArtifactRepositories()))

        result.setExcludedPaths(getExcludedPath(mavenProject));
        result.setGeneratedPath(getGeneratedPath(mavenProject));
        result.setTestGeneratedPath(getGeneratedTestPath(mavenProject));
        result.setAnnotationProcessorPaths(getAnnotationProcessors(pluginWrappers));
        return result;
    }

    private static void addReferencedProjects(
            org.apache.maven.project.MavenProject mavenProject, List<MavenArtifact> artifacts, int depth
    ) {
        if (depth > 2) return;
        Map<String, org.apache.maven.project.MavenProject> references = mavenProject.getProjectReferences();
        if (references != null) {
            for (org.apache.maven.project.MavenProject each : references.values()) {
                MavenArtifact mavenArtifact = MavenArtifactConverter.convert(each);
                artifacts.add(mavenArtifact);
               // addReferencedProjects(each, artifacts, depth + 1);
            }
        }
    }

    private static List<String> getExcludedPath(org.apache.maven.project.MavenProject project) {
        Object contextValue = project.getContextValue("GMaven:excludedPaths");
        if (contextValue instanceof List) {
            return (List<String>) contextValue;
        }
        return Collections.emptyList();
    }

    private static String getGeneratedPath(org.apache.maven.project.MavenProject project) {
        Object contextValue = project.getContextValue("GMaven:generatedPath");
        if (contextValue instanceof String && !((String) contextValue).isEmpty()) {
            return (String) contextValue;
        }
        return Paths.get(project.getBuild().getDirectory(), "generated-sources", "annotations").toString();
    }

    private static String getGeneratedTestPath(org.apache.maven.project.MavenProject project) {
        Object contextValue = project.getContextValue("GMaven:generatedTestPath");
        if (contextValue instanceof String && !((String) contextValue).isEmpty()) {
            return (String) contextValue;
        }
        return Paths.get(project.getBuild().getDirectory(), "generated-test-sources", "test-annotations").toString();
    }

    private static Map<Object, Object> getProperties(org.apache.maven.project.MavenProject mavenProject) {
        Properties projectProperties = mavenProject.getProperties();
        if (projectProperties == null) {
            return Collections.emptyMap();
        } else {
            HashMap<Object, Object> result = new HashMap<>(projectProperties.size());
            result.putAll(projectProperties);
            return result;
        }
    }

    private static List<String> convertModules(File basedir, List<String> modules) {
        if (modules == null || modules.isEmpty()) return Collections.emptyList();
        ArrayList<String> result = new ArrayList<>(modules.size());
        for (String module : modules) {
            result.add(MavenProjectContainerConverter.getModuleFile(basedir, module).getAbsolutePath());
        }
        return result;
    }

    private static List<MavenResource> convertResource(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) return Collections.emptyList();
        ArrayList<MavenResource> result = new ArrayList<>(resources.size());
        for (Resource item : resources) {
            MavenResource resource = new MavenResource();
            resource.setDirectory(item.getDirectory());
            result.add(resource);
        }
        return result;
    }

    private static List<String> getAnnotationProcessors(List<MavenPluginWrapper> plugins) {
        ArrayList<String> result = new ArrayList<>(1);
        for (MavenPluginWrapper plugin : plugins) {
            if (plugin.getBody() != null) {
                result.addAll(plugin.getBody().getAnnotationProcessorPaths());
            }
        }
        return result;
    }
}
