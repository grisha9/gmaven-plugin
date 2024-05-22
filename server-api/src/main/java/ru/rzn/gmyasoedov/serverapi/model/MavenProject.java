package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder
public class MavenProject extends MavenId {
    static final long serialVersionUID = -3453607480882347421L;

    private final String name;
    private final String packaging;
    private final File file;
    private final File parentFile;
    private final String basedir;
    private final MavenArtifact parentArtifact;
    private final List<MavenPlugin> plugins;
    private final List<DependencyTreeNode> dependencyTree;
    private final List<String> modulesDir;
    private final List<String> sourceRoots;
    private final List<String> testSourceRoots;
    private final List<String> resourceRoots;
    private final List<String> testResourceRoots;
    private final List<MavenArtifact> resolvedArtifacts;
    private final List<MavenArtifact> dependencyArtifacts;
    private final String buildDirectory;//target dir
    private final String outputDirectory;
    private final String testOutputDirectory;
    private final Map<Object, Object> properties;
    private final List<MavenRemoteRepository> remoteRepositories;

    private final List<String> excludedPaths;
    private final String generatedPath;
    private final String testGeneratedPath;

    public @NotNull String getDisplayName() {
        return (name == null || name.isEmpty()) ? artifactId : name;
    }
}
