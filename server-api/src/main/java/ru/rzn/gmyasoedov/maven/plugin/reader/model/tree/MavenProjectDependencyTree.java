package ru.rzn.gmyasoedov.maven.plugin.reader.model.tree;

import java.util.List;

public class MavenProjectDependencyTree {

    public String groupId;
    public String artifactId;
    public String version;
    public List<DependencyTreeNode> dependencies;

}
