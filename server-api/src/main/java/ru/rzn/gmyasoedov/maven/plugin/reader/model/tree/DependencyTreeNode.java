package ru.rzn.gmyasoedov.maven.plugin.reader.model.tree;

import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenArtifactNode;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenArtifactState;

import java.io.Serializable;
import java.util.List;

public class DependencyTreeNode implements Serializable {

    public MavenArtifactNode artifact;
    @Nullable
    public MavenArtifactNode relatedArtifact;
    public MavenArtifactState state;
    @Nullable
    public String originalScope;
    public List<DependencyTreeNode> dependencies;

}
