// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.serverapi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DependencyTreeNode implements Serializable {
    @Nullable
    private DependencyTreeNode parent;
    private MavenArtifactNode artifact;
    private MavenArtifactState state;
    @Nullable
    private MavenArtifactNode relatedArtifact;
    @Nullable
    private String originalScope;
    private List<DependencyTreeNode> dependencies;

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
