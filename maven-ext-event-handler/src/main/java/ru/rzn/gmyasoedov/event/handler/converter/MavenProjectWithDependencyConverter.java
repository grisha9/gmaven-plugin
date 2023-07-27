package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;

import java.util.List;

public class MavenProjectWithDependencyConverter {

    public static MavenProjectContainer convert(EventSpyResultHolder source) {
        if (source.dependencyResult == null || source.dependencyResult.isEmpty()
                || source.executionResult == null
                || source.session == null
                || source.executionResult.getTopologicallySortedProjects() == null) return null;
        MavenExecutionResult executionResult = source.executionResult;
        List<String> selectedProjects = source.session.getRequest().getSelectedProjects();
        if (selectedProjects == null || selectedProjects.isEmpty()) return null;
        String selectedProject = selectedProjects.get(0);
        List<MavenProject> allProjects = executionResult.getTopologicallySortedProjects();
        if (allProjects == null) return null;
        for (MavenProject project : allProjects) {
            if (selectedProject.endsWith(project.getArtifactId())) {
                return new MavenProjectContainer(MavenProjectConverter.convert(project, source.session));
            }
        }
        return null;
    }
}
