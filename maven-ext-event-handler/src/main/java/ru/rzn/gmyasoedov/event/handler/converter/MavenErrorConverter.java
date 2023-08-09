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
import ru.rzn.gmyasoedov.serverapi.model.BuildErrors;
import ru.rzn.gmyasoedov.serverapi.model.MavenException;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MavenErrorConverter {

    public static BuildErrors convert(MavenExecutionResult result) {
        List<Throwable> exceptions = result.getExceptions();
        if (exceptions == null || exceptions.isEmpty())
            return new BuildErrors(false, Collections.<MavenException>emptyList());

        boolean pluginNotResolved = false;
        List<MavenException> mavenExceptions = new ArrayList<>(exceptions.size());
        for (Throwable each : exceptions) {
            Throwable rootCause = getRootError(each);
            if (each instanceof PluginResolutionException) {
                PluginResolutionException e = (PluginResolutionException) each;
                Plugin plugin = e.getPlugin();
                if (plugin != null && "ru.rzn.gmyasoedov".equalsIgnoreCase(plugin.getGroupId())
                        && "model-reader".equalsIgnoreCase(plugin.getArtifactId())) {
                    pluginNotResolved = true;
                } else if (plugin != null) {
                    mavenExceptions.add(new MavenException(e.getMessage(), toMavenId(plugin), null));
                }
            } else if (rootCause instanceof ArtifactTransferException) {
                Artifact artifact = ((ArtifactTransferException) rootCause).getArtifact();
                String message = rootCause.getMessage() != null ? rootCause.getMessage() : each.getMessage();
                mavenExceptions.add(new MavenException(message, toMavenId(artifact), null));
            } else if (each instanceof ArtifactTransferException) {
                Artifact artifact = ((ArtifactTransferException) each).getArtifact();
                mavenExceptions.add(new MavenException(each.getMessage(), toMavenId(artifact), null));
            } else if (each instanceof ProjectBuildingException) {
                List<ProjectBuildingResult> results = ((ProjectBuildingException) each).getResults();
                for (ProjectBuildingResult buildingResult : results) {
                    for (ModelProblem problem : buildingResult.getProblems()) {
                        mavenExceptions.add(toMavenException(problem));
                    }
                }
            } else {
                mavenExceptions.add(new MavenException(each.getMessage(), null, null));
            }
        }
        return new BuildErrors(pluginNotResolved, mavenExceptions);
    }

    private static MavenException toMavenException(ModelProblem problem) {
        String message = problem.getMessage();
        String source = problem.getSource();
        int lineNumber = problem.getLineNumber();
        int columnNumber = problem.getColumnNumber();
        String messageWithCoordinate = String.format("%s:%s:%s", source, lineNumber, columnNumber);
        message = message.replace(source, messageWithCoordinate);
        return new MavenException(message, null, source);
    }

    private static MavenId toMavenId(Plugin plugin) {
        if (plugin == null) return null;
        return new MavenId(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
    }

    private static MavenId toMavenId(Artifact artifact) {
        if (artifact == null) return null;
        return new MavenId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
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
