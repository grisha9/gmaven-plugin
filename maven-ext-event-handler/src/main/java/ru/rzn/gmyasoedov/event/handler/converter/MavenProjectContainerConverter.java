package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MavenProjectContainerConverter {

    public static MavenProjectContainer convert(EventSpyResultHolder source) {
        MavenExecutionResult executionResult = source.executionResult;
        List<MavenProject> allProjects = getAllProjects(source);
        Map<File, MavenProject> projectByDirectoryMap = getMapForProjects(allProjects);
        for (MavenProject sortedProject : allProjects) {
            projectByDirectoryMap.put(sortedProject.getBasedir(), sortedProject);
        }

        MavenProject topLevelProject = executionResult.getProject();
        if (topLevelProject == null) {
            return null;
        }
        boolean readOnly = source.session.getRequest().getGoals().contains(GMavenServer.READ_TASK);
        MavenProjectContainer container = new MavenProjectContainer(MavenProjectConverter
                .convert(topLevelProject, source.dependencyResult, readOnly));
        fillContainer(container, projectByDirectoryMap, source, readOnly);

        return container;
    }

    private static List<MavenProject> getAllProjects(EventSpyResultHolder source) {
        if (source.session == null) return Collections.emptyList();
        return source.session.getAllProjects() == null
                ? Collections.<MavenProject>emptyList() : source.session.getAllProjects();
    }

    private static Map<File, MavenProject> getMapForProjects(List<MavenProject> projects) {
        if (projects.size() > 128) {
            return new TreeMap<>();
        } else {
            return new HashMap<>(projects.size() + 1);
        }
    }

    private static void fillContainer(MavenProjectContainer rootContainer,
                                      Map<File, MavenProject> projectByDirectoryMap,
                                      EventSpyResultHolder resultHolder,
                                      boolean readOnly) {
        ru.rzn.gmyasoedov.serverapi.model.MavenProject project = rootContainer.getProject();
        for (String module : project.getModulesDir()) {
            if (StringUtils.isEmpty(module)) continue;

            File moduleFile = new File(module);
            MavenProject mavenProjectByModuleFile = projectByDirectoryMap.get(moduleFile);
            if (mavenProjectByModuleFile == null) continue;

            MavenProjectContainer projectContainer = new MavenProjectContainer(
                    MavenProjectConverter.convert(mavenProjectByModuleFile, resultHolder.dependencyResult, readOnly)
            );
            rootContainer.getModules().add(projectContainer);
            fillContainer(projectContainer, projectByDirectoryMap, resultHolder, readOnly);
        }
    }

    static File getModuleFile(File parentProjectFile, String relativePath) {
        relativePath = relativePath.replace('\\', File.separatorChar).replace('/', File.separatorChar);
        File moduleFile = new File(parentProjectFile, relativePath);
        if (moduleFile.isFile()) {
            moduleFile = moduleFile.getParentFile();
        }
        // we don't canonicalize on unix to avoid interfering with symlinks
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            try {
                moduleFile = moduleFile.getCanonicalFile();
            } catch (IOException e) {
                moduleFile = moduleFile.getAbsoluteFile();
            }
        } else {
            moduleFile = new File(moduleFile.toURI().normalize());
        }
        return moduleFile;
    }
}
