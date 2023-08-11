package ru.rzn.gmyasoedov.event.handler;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.graph.DependencyNode;
import ru.rzn.gmyasoedov.event.handler.converter.MavenErrorConverter;
import ru.rzn.gmyasoedov.event.handler.converter.MavenProjectContainerConverter;
import ru.rzn.gmyasoedov.event.handler.converter.MavenProjectWithDependencyConverter;
import ru.rzn.gmyasoedov.event.handler.converter.MavenSettingsConverter;
import ru.rzn.gmyasoedov.gmaven.server.result.ResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.BuildErrors;
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;
import ru.rzn.gmyasoedov.serverapi.model.MavenResult;

import javax.inject.Named;

import static ru.rzn.gmyasoedov.serverapi.GMavenServer.GMAVEN_DEPENDENCY_TREE;

@Named
public class GMavenEventSpy extends AbstractEventSpy {
    private static EventSpyResultHolder resultHolder;

    @Override
    public void onEvent(Object event) {
        if (event instanceof DefaultSettingsBuildingRequest) {
            resultHolder = new EventSpyResultHolder();
        } else if (event instanceof SettingsBuildingResult) {
            resultHolder.settingsActiveProfiles = ((SettingsBuildingResult) event)
                    .getEffectiveSettings().getActiveProfiles();
        } else if (event instanceof ExecutionEvent) {
            if (((ExecutionEvent) event).getSession() != null) {
                resultHolder.session = ((ExecutionEvent) event).getSession();
            }
        } else if (event instanceof DependencyResolutionResult) {
            if (isDependencyAnalyzer()) {
                DependencyNode dependencyGraph = ((DependencyResolutionResult) event).getDependencyGraph();
                String key = dependencyGraph.getArtifact().getArtifactId();
                resultHolder.dependencyResult.put(key, dependencyGraph.getChildren());
            }
        } else if (event instanceof MavenExecutionResult) {
            resultHolder.executionResult = (MavenExecutionResult) event;
            setResult();
        }
    }

    private static boolean isDependencyAnalyzer() {
        if (resultHolder.session == null) return false;
        return resultHolder.session.getSystemProperties().getProperty(GMAVEN_DEPENDENCY_TREE, "")
                .equalsIgnoreCase("true")
                || resultHolder.session.getUserProperties().getProperty(GMAVEN_DEPENDENCY_TREE, "")
                .equalsIgnoreCase("true");
    }

    private void setResult() {
        BuildErrors buildErrors = MavenErrorConverter.convert(resultHolder.executionResult);
        ResultHolder.result = new MavenResult(
                buildErrors.pluginNotResolved,
                MavenSettingsConverter.convert(resultHolder),
                getProjectContainer(),
                buildErrors.exceptions
        );
    }

    private static MavenProjectContainer getProjectContainer() {
        return resultHolder.dependencyResult.isEmpty()
                ? MavenProjectContainerConverter.convert(resultHolder)
                : MavenProjectWithDependencyConverter.convert(resultHolder);
    }
}
