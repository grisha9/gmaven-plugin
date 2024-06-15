package ru.rzn.gmyasoedov.serverapi.model;

import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenArtifactNode;

import java.io.Serializable;
import java.util.List;

public class DependencyTreeNode implements Serializable {
    @Nullable
    public DependencyTreeNode parent;
    public MavenArtifactNode artifact;
    public MavenArtifactState state;
    @Nullable
    public MavenArtifactNode relatedArtifact;
    @Nullable
    public String originalScope;
    public List<DependencyTreeNode> dependencies;


    public DependencyTreeNode(
            @Nullable DependencyTreeNode parent,
            MavenArtifactNode artifact,
            MavenArtifactState state,
            @Nullable MavenArtifactNode relatedArtifact,
            @Nullable String originalScope,
            List<DependencyTreeNode> dependencies) {
        this.parent = parent;
        this.artifact = artifact;
        this.state = state;
        this.relatedArtifact = relatedArtifact;
        this.originalScope = originalScope;
        this.dependencies = dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyTreeNode that = (DependencyTreeNode) o;

        return artifact.equals(that.artifact);
    }

    @Override
    public String toString() {
        return "DependencyTreeNode{" +
                "artifact=" + artifact +
                '}';
    }

    @Override
    public int hashCode() {
        return artifact.hashCode();
    }
}
