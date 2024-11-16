package ru.rzn.gmyasoedov.event.spy;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.util.ExceptionUtils;
import org.eclipse.aether.transfer.ArtifactTransferException;
import ru.rzn.gmyasoedov.event.spy.model.BuildErrors;

import java.util.ArrayList;
import java.util.List;

public class MavenErrorConverter {

    public static BuildErrors convert(MavenExecutionResult result) {
        List<Throwable> exceptions = result.getExceptions();
        if (exceptions == null || exceptions.isEmpty())
            return new BuildErrors();

        boolean pluginNotResolved = false;
        List<String> mavenExceptions = new ArrayList<>(exceptions.size());
        for (Throwable each : exceptions) {
            Throwable rootCause = getRootError(each);
            if (each instanceof PluginResolutionException) {
                PluginResolutionException e = (PluginResolutionException) each;
                Plugin plugin = e.getPlugin();
                if (plugin == null) continue;
                if (isGMavenPlugin(plugin)) {
                    pluginNotResolved = true;
                } else {
                    mavenExceptions.add(getMavenException(e.getMessage()));
                }
            } else if (rootCause instanceof ArtifactTransferException) {
                String message = rootCause.getMessage() != null ? rootCause.getMessage() : each.getMessage();
                mavenExceptions.add(getMavenException(message));
            } else if (each instanceof ArtifactTransferException) {
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
        BuildErrors errors = new BuildErrors();
        errors.pluginNotResolved = pluginNotResolved;
        errors.exceptions = mavenExceptions;
        return errors;
    }

    private static boolean isGMavenPlugin(Plugin plugin) {
        String groupId = plugin.getGroupId();
        return groupId != null
                && (groupId.contains("ru.rzn.gmyasoedov") || groupId.contains("io.github.grisha9"))
                && "maven-model-reader-plugin".equalsIgnoreCase(plugin.getArtifactId());
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

    private static Throwable getRootError(Throwable each) {
        try {
            return ExceptionUtils.getRootCause(each);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return each;
    }
}
