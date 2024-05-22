package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.eclipse.aether.graph.DependencyNode;
import ru.rzn.gmyasoedov.serverapi.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public class MavenProjectConverter {

    public static MavenProject convert(org.apache.maven.project.MavenProject mavenProject,
                                       Map<String, List<DependencyNode>> dependencyResultMap,
                                       boolean readOnly) {
        if (dependencyResultMap == null) {
            dependencyResultMap = Collections.emptyMap();
        }

        List<MavenPlugin> plugins = new ArrayList<>(mavenProject.getBuildPlugins().size());
        for (Plugin plugin : mavenProject.getBuildPlugins()) {
            plugins.add(MavenPluginConverter.convert(plugin, mavenProject));
        }
        List<MavenArtifact> artifacts = new ArrayList<>(mavenProject.getArtifacts().size());

        Map<Artifact, MavenArtifact> convertedArtifactMap = new HashMap<>(mavenProject.getArtifacts().size());
        for (Artifact artifact : mavenProject.getArtifacts()) {
            MavenArtifact mavenArtifact = MavenArtifactConverter.convert(artifact);
            artifacts.add(mavenArtifact);
            convertedArtifactMap.put(artifact, mavenArtifact);
        }
        if (readOnly) {
            Map<String, org.apache.maven.project.MavenProject> references = mavenProject.getProjectReferences();
            if (references != null) {
                for (org.apache.maven.project.MavenProject each : references.values()) {
                    MavenArtifact mavenArtifact = MavenArtifactConverter.convert(each);
                    artifacts.add(mavenArtifact);
                }
            }
        }
        List<DependencyTreeNode> dependencyTreeNodes = DependencyTreeNodeConverter
                .convert(dependencyResultMap.get(mavenProject.getArtifactId()), convertedArtifactMap);
        List<String> modulesDir = convertModules(mavenProject.getBasedir(), mavenProject.getModules());

        return MavenProject.builder()
                .groupId(mavenProject.getGroupId())
                .artifactId(mavenProject.getArtifactId())
                .version(mavenProject.getVersion())
                .packaging(mavenProject.getPackaging())
                .name(mavenProject.getName())
                .basedir(mavenProject.getBasedir().getAbsolutePath())
                .file(mavenProject.getFile())
                .parentFile(mavenProject.getParentFile())
                .modulesDir(modulesDir)
                .plugins(plugins)
                .dependencyTree(dependencyTreeNodes)
                .sourceRoots(mavenProject.getCompileSourceRoots())
                .testSourceRoots(mavenProject.getTestCompileSourceRoots())
                .resourceRoots(convertResource(mavenProject.getResources()))
                .testResourceRoots(convertResource(mavenProject.getTestResources()))
                .buildDirectory(mavenProject.getBuild().getDirectory())
                .outputDirectory(mavenProject.getBuild().getOutputDirectory())
                .testOutputDirectory(mavenProject.getBuild().getTestOutputDirectory())
                .resolvedArtifacts(artifacts)
                .parentArtifact(mavenProject.getParent() != null
                        ? MavenArtifactConverter.convert(mavenProject.getParent()) : null)
                .properties(getProperties(mavenProject))
                .remoteRepositories(Collections.<MavenRemoteRepository>emptyList())
                //.remoteRepositories(RemoteRepositoryConverter.convert(mavenProject.getRemoteArtifactRepositories()))

                .excludedPaths(getExcludedPath(mavenProject))
                .generatedPath(getGeneratedPath(mavenProject))
                .testGeneratedPath(getGeneratedTestPath(mavenProject))
                .build();
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

    private static List<String> convertResource(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) return Collections.emptyList();
        ArrayList<String> result = new ArrayList<>(resources.size());
        for (Resource item : resources) {
            result.add(item.getDirectory());
        }
        return result;
    }

    private static List<MavenArtifact> convertMavenArtifact(Set<Artifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) return Collections.emptyList();
        ArrayList<MavenArtifact> result = new ArrayList<>(artifacts.size());
        for (Artifact item : artifacts) {
            result.add(MavenArtifactConverter.convert(item));
        }
        return result;
    }
}
