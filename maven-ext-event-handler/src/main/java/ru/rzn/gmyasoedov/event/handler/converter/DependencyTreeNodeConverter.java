package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import ru.rzn.gmyasoedov.serverapi.model.DependencyTreeNode;
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact;
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifactState;

import java.util.*;

public class DependencyTreeNodeConverter {

    public static List<DependencyTreeNode> convert(
            Collection<? extends DependencyNode> nodes,
            Map<Artifact, MavenArtifact> convertedArtifactMap) {
        if (nodes == null || nodes.isEmpty()) return Collections.emptyList();
        return convert(null, nodes, convertedArtifactMap);
    }

    private static List<DependencyTreeNode> convert(
            DependencyTreeNode parent,
            Collection<? extends DependencyNode> nodes,
            Map<Artifact, MavenArtifact> convertedArtifactMap) {
        List<DependencyTreeNode> result = new ArrayList<>(nodes.size());
        for (DependencyNode each : nodes) {
            Artifact artifactNode = toArtifact(each.getDependency());
            MavenArtifactState state = MavenArtifactState.ADDED;
            Object winner = each.getData().get(ConflictResolver.NODE_DATA_WINNER);

            MavenArtifact winnerArtifact = null;
            MavenArtifact mavenArtifact;

            if (winner instanceof DependencyNode) {
                DependencyNode winnerNode = (DependencyNode) winner;
                winnerArtifact = MavenArtifactConverter.convert(toArtifact(winnerNode.getDependency()));
                mavenArtifact = MavenArtifactConverter.convert(artifactNode);
                if (!Objects.equals(each.getVersion().toString(), winnerNode.getVersion().toString())) {
                    state = MavenArtifactState.CONFLICT;
                } else {
                    state = MavenArtifactState.DUPLICATE;
                }
            } else {
                mavenArtifact = getArtifact(artifactNode, convertedArtifactMap);
            }

            String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(each);
            String premanagedScope = DependencyManagerUtils.getPremanagedScope(each);
            DependencyTreeNode newNode = new DependencyTreeNode(parent, mavenArtifact,
                    state, winnerArtifact, each.getDependency().getScope(), premanagedVersion, premanagedScope
            );
            newNode.setDependencies(convert(newNode, each.getChildren(), convertedArtifactMap));
            result.add(newNode);
        }
        return result;
    }

    public static Artifact toArtifact(Dependency dependency) {
        if (dependency == null) {
            return null;
        }

        Artifact result = RepositoryUtils.toArtifact(dependency.getArtifact());
        if (result == null) {
            return null;
        }
        result.setScope(dependency.getScope());
        result.setOptional(dependency.isOptional());
        return result;
    }

    public static MavenArtifact getArtifact(Artifact artifact, Map<Artifact, MavenArtifact> convertedArtifactMap) {
        MavenArtifact result = convertedArtifactMap.get(artifact);
        if (result == null) {
            result = MavenArtifactConverter.convert(artifact);
            convertedArtifactMap.put(artifact, result);
        }
        return result;
    }
}
