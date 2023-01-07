// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.serverapi.model;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DependencyTreeNode implements Serializable {
    private final DependencyTreeNode parent;
    private final MavenArtifact artifact;
    private final MavenArtifactState state;
    private final MavenArtifact relatedArtifact;
    private final String originalScope;
    private final String premanagedVersion;
    private final String premanagedScope;

    private List<DependencyTreeNode> myDependencies;

    public DependencyTreeNode(DependencyTreeNode parent,
                              MavenArtifact artifact,
                              MavenArtifactState state,
                              MavenArtifact relatedArtifact,
                              String originalScope,
                              String premanagedVersion,
                              String premanagedScope) {
        this.parent = parent;
        this.artifact = artifact;
        this.state = state;
        this.relatedArtifact = relatedArtifact;
        this.originalScope = originalScope;
        this.premanagedVersion = premanagedVersion;
        this.premanagedScope = premanagedScope;
    }

    @Nullable
    public DependencyTreeNode getParent() {
        return parent;
    }

    public MavenArtifact getArtifact() {
        return artifact;
    }

    public MavenArtifactState getState() {
        return state;
    }

    @Nullable
    public MavenArtifact getRelatedArtifact() {
        return relatedArtifact;
    }

    @Nullable
    public String getOriginalScope() {
        return originalScope;
    }

    @Nullable
    public String getPremanagedVersion() {
        return premanagedVersion;
    }

    @Nullable
    public String getPremanagedScope() {
        return premanagedScope;
    }

    public List<DependencyTreeNode> getDependencies() {
        return myDependencies;
    }

    public void setDependencies(List<DependencyTreeNode> dependencies) {
        myDependencies = new ArrayList<DependencyTreeNode>(dependencies);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyTreeNode that = (DependencyTreeNode) o;

        return artifact.equals(that.artifact);
    }

    @Override
    public int hashCode() {
        return artifact.hashCode();
    }
}
