package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.util.ExceptionUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.transfer.ArtifactTransferException;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.BuildErrors;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenId;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.ObjectUtils;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.SimpleMavenId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MavenErrorConverter {

    public static BuildErrors convert(MavenExecutionResult result) {
        List<Throwable> exceptions = result.getExceptions();
        if (exceptions == null || exceptions.isEmpty()) {
            BuildErrors buildErrors = new BuildErrors();
            buildErrors.setExceptions(Collections.<String>emptyList());
            buildErrors.setPluginNotResolved(false);
            return buildErrors;
        }

        boolean pluginNotResolved = false;
        List<String> mavenExceptions = new ArrayList<>(exceptions.size());
        for (Throwable each : exceptions) {
            Throwable rootCause = getRootError(each);
            if (each instanceof PluginResolutionException) {
                PluginResolutionException e = (PluginResolutionException) each;
                Plugin plugin = e.getPlugin();
                if (plugin != null && "ru.rzn.gmyasoedov".equalsIgnoreCase(plugin.getGroupId())
                        && "model-reader".equalsIgnoreCase(plugin.getArtifactId())) {
                    pluginNotResolved = true;
                } else if (plugin != null) {
                    mavenExceptions.add(getMavenException(e.getMessage()));
                }
            } else if (rootCause instanceof ArtifactTransferException) {
                Artifact artifact = ((ArtifactTransferException) rootCause).getArtifact();
                String message = rootCause.getMessage() != null ? rootCause.getMessage() : each.getMessage();
                mavenExceptions.add(getMavenException(message));
            } else if (each instanceof ArtifactTransferException) {
                Artifact artifact = ((ArtifactTransferException) each).getArtifact();
                mavenExceptions.add(getMavenException(each.getMessage()));
            } else if (each instanceof ProjectBuildingException) {
                List<ProjectBuildingResult> results = ((ProjectBuildingException) each).getResults();
                for (ProjectBuildingResult buildingResult : results) {
                    for (ModelProblem problem : buildingResult.getProblems()) {
                        mavenExceptions.add(toMavenException(problem));
                    }
                }
            } else {
                String rootMessage = rootCause != null ? rootCause.getMessage() : null;
                String message = rootMessage != null ? rootMessage : each.getMessage();
                mavenExceptions.add(getMavenException(message));
            }
        }
        BuildErrors buildErrors = new BuildErrors();
        buildErrors.setPluginNotResolved(pluginNotResolved);
        buildErrors.setExceptions(mavenExceptions);
        return buildErrors;
    }

    private static String toMavenException(ModelProblem problem) {
        String message = problem.getMessage();
        String source = problem.getSource();
        int lineNumber = problem.getLineNumber();
        int columnNumber = problem.getColumnNumber();
        String messageWithCoordinate = String.format("%s:%s:%s", source, lineNumber, columnNumber);
        message = message.replace(source, messageWithCoordinate);
        return getMavenException(message.replace("\"", ""));
    }

    private static String getMavenException(String message) {
        if (message == null) return "Unknown error. See Maven log";
        return message.replace("\"", "");
    }

    private static MavenId toMavenId(Plugin plugin) {
        if (plugin == null) return null;
        SimpleMavenId result = new SimpleMavenId();
        result.setGroupId(ObjectUtils.defaultIfNull(plugin.getGroupId(), SimpleMavenId.UNKNOWN_VALUE));
        result.setArtifactId(ObjectUtils.defaultIfNull(plugin.getArtifactId(), SimpleMavenId.UNKNOWN_VALUE));
        result.setVersion(ObjectUtils.defaultIfNull(plugin.getVersion(), SimpleMavenId.UNKNOWN_VALUE));
        return result;
    }

    private static MavenId toMavenId(Artifact artifact) {
        if (artifact == null) return null;
        SimpleMavenId result = new SimpleMavenId();
        result.setGroupId(ObjectUtils.defaultIfNull(artifact.getGroupId(), SimpleMavenId.UNKNOWN_VALUE));
        result.setArtifactId(ObjectUtils.defaultIfNull(artifact.getArtifactId(), SimpleMavenId.UNKNOWN_VALUE));
        result.setVersion(ObjectUtils.defaultIfNull(artifact.getVersion(), SimpleMavenId.UNKNOWN_VALUE));
        return result;
    }

    private static Throwable getRootError(Throwable each) {
        try {
            return ExceptionUtils.getRootCause(each);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return each;
    }
}
