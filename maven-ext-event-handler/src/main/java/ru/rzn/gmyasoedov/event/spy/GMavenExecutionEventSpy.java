package ru.rzn.gmyasoedov.event.spy;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DependencyNode;
import ru.rzn.gmyasoedov.event.spy.model.BuildErrors;

import javax.inject.Named;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static ru.rzn.gmyasoedov.event.spy.GMavenEventSpyConstants.*;

@Named
public class GMavenExecutionEventSpy extends AbstractEventSpy {

    private MavenSession session;

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent) {
            if (((ExecutionEvent) event).getSession() != null) {
                session = ((ExecutionEvent) event).getSession();
            }
        } else if (event instanceof DependencyResolutionResult) {
            if (!isTreeGoal()) return;
            DependencyNode dependencyGraph = ((DependencyResolutionResult) event).getDependencyGraph();
            setGraphToProjectContext(dependencyGraph);
        } else if (event instanceof MavenExecutionResult) {
            if (!isModelReadGoal()) return;
            List<Throwable> exceptions = ((MavenExecutionResult) event).getExceptions();
            if (exceptions == null || exceptions.isEmpty()) return;
            String resultErrorJson = getErrorJson((MavenExecutionResult) event);
            String resultFilePath = getResultFilePath(session);
            printErrorResult(resultErrorJson, resultFilePath);
        }
    }

    private void setGraphToProjectContext(DependencyNode dependencyGraph) {
        if (session == null) return;
        String key = dependencyGraph.getArtifact().getArtifactId();
        if (key == null) return;
        List<MavenProject> projects = session.getProjects();
        if (projects == null) return;
        for (MavenProject project : projects) {
            if (key.equals(project.getArtifactId())) {
                project.setContextValue(GMAVEN_DEPENDENCY_GRAPH, dependencyGraph);
                return;
            }
        }
    }

    private String errorToJsonArray(List<String> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) return "[]";
        if (exceptions.size() == 1) return "[\"" + exceptions.get(0) + "\"]";
        StringBuilder builder = new StringBuilder(exceptions.get(0));
        for (int i = 1; i < exceptions.size(); i++) {
            builder.append("\"");
            builder.append(exceptions.get(i));
            builder.append("\",");
        }
        return "[" + builder + "]";
    }

    private boolean isTreeGoal() {
        if (session == null) return false;
        List<String> goals = session.getGoals();
        if (goals == null) return false;
        for (String goal : goals) {
            if (goal.contains(MAVEN_MODEL_READER_PLUGIN) && goal.endsWith("tree")) {
                return true;
            }
        }
        return false;
    }

    private boolean isModelReadGoal() {
        if (session == null) return false;
        List<String> goals = session.getGoals();
        if (goals == null) return false;
        for (String goal : goals) {
            if (goal.contains(MAVEN_MODEL_READER_PLUGIN)
                    && (goal.endsWith("read") || goal.endsWith("resolve"))) {
                return true;
            }
        }
        return false;
    }

    private String getErrorJson(MavenExecutionResult event) {
        BuildErrors buildErrors = MavenErrorConverter.convert(event);
        String errorJsonArray = errorToJsonArray(buildErrors.exceptions);
        boolean pluginNotResolved = buildErrors.pluginNotResolved;
        return  "{\"pluginNotResolved\": " + pluginNotResolved + ",  \"exceptions\": " + errorJsonArray + "}";
    }

    protected void printErrorResult(String result, String resultFilePath) {
        Path resultPath = Paths.get(resultFilePath);
        if (resultPath.toFile().isDirectory()) {
            throw new RuntimeException("Parameter resultFilePath is directory! Must be a file.");
        }
        Path buildDirectory = resultPath.getParent();
        try {
            if (!buildDirectory.toFile().exists()) {
                Files.createDirectory(buildDirectory);
            }
            try (Writer writer = new FileWriter(resultPath.toFile())) {
                writer.write(result);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getResultFilePath(MavenSession session) {
        String resultFilePath = session.getUserProperties().getProperty(RESULT_FILE_PATH);
        if (resultFilePath != null) return resultFilePath;
        resultFilePath = session.getSystemProperties().getProperty(RESULT_FILE_PATH);
        return resultFilePath;
    }
}
