package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;

import java.util.ArrayList;
import java.util.List;

public class MavenProjectWithDependencyConverter {

    public static MavenProjectContainer convert(EventSpyResultHolder source) {
        if (source.dependencyResult == null || source.dependencyResult.isEmpty()
                || source.executionResult == null
                || source.session == null
                || source.executionResult.getTopologicallySortedProjects() == null) return null;
        MavenExecutionResult executionResult = source.executionResult;
        List<MavenProject> allProjects = executionResult.getTopologicallySortedProjects();
        if (allProjects == null) return null;
        List<MavenProjectContainer> result = new ArrayList<>(allProjects.size());
        for (MavenProject project : allProjects) {
            result.add(new MavenProjectContainer(
                    MavenProjectConverter.convert(project, source.dependencyResult))
            );
        }
        if (result.isEmpty()) return null;
        MavenProjectContainer projectContainer = new MavenProjectContainer(result.get(0).getProject());
        projectContainer.getModules().addAll(result);
        return projectContainer;
    }
}
