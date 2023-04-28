package ru.rzn.gmyasoedov.event.handler;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.eclipse.aether.graph.DependencyNode;
import ru.rzn.gmyasoedov.event.handler.converter.MavenProjectContainerConverter;
import ru.rzn.gmyasoedov.gmaven.server.result.ResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.MavenException;
import ru.rzn.gmyasoedov.serverapi.model.MavenResult;

import javax.inject.Named;
import java.util.Collections;
import java.util.List;

@Named
public class GMavenEventSpy extends AbstractEventSpy {
    public static final String DEPENDENCY_RESULT_MAP = "dependencyResultMap";
    private static EventSpyResultHolder resultHolder;

    @Override
    public void onEvent(Object event) {
        if (event instanceof DefaultSettingsBuildingRequest) {
            resultHolder = new EventSpyResultHolder();
        } else if (event instanceof ExecutionEvent) {
            if (((ExecutionEvent) event).getType() == ExecutionEvent.Type.MojoSucceeded) {
                resultHolder.session = ((ExecutionEvent) event).getSession();
            }
        } else if (event instanceof DependencyResolutionResult) {
            DependencyNode dependencyGraph = ((DependencyResolutionResult) event).getDependencyGraph();
            String key = dependencyGraph.getArtifact().getArtifactId();
            resultHolder.dependencyResult.put(key, dependencyGraph.getChildren());
        } else if (event instanceof MavenExecutionResult) {
            resultHolder.executionResult = (MavenExecutionResult) event;
            if (resultHolder.session != null) {
                resultHolder.session.getUserProperties().put(DEPENDENCY_RESULT_MAP, resultHolder.dependencyResult);
            }
            setResult();
        }
    }

    private void setResult() {
        boolean pluginResolutionError = isGPluginResolutionError(resultHolder.executionResult);
        String localRepository = resultHolder.session != null
                ? resultHolder.session.getLocalRepository().getBasedir() : null;
        ResultHolder.result = new MavenResult(
                pluginResolutionError,
                localRepository,
                MavenProjectContainerConverter.convert(resultHolder),
                Collections.<MavenException>emptyList()
        );
    }

    private boolean isGPluginResolutionError(MavenExecutionResult result) {
        List<Throwable> exceptions = result.getExceptions();
        if (exceptions == null) return false;
        for (Throwable exception : exceptions) {
            if (exception instanceof PluginResolutionException) {
                PluginResolutionException e = (PluginResolutionException) exception;
                if (e.getPlugin() != null && "ru.rzn.gmyasoedov".equalsIgnoreCase(e.getPlugin().getGroupId())
                        && "model-reader".equalsIgnoreCase(e.getPlugin().getArtifactId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
