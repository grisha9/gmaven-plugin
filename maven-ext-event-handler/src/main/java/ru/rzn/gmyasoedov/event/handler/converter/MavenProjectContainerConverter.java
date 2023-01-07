package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MavenProjectContainerConverter {

    public static MavenProjectContainer convert(EventSpyResultHolder source) {
        MavenExecutionResult executionResult = source.executionResult;
        Map<File, MavenProject> projectByDirectoryMap = getMapForProjects(executionResult);
        List<MavenProject> sortedProjects = executionResult.getTopologicallySortedProjects();
        for (MavenProject sortedProject : sortedProjects) {
            projectByDirectoryMap.put(sortedProject.getBasedir(), sortedProject);
        }

        MavenProject topLevelProject = executionResult.getProject();
        MavenProjectContainer container = new MavenProjectContainer(MavenProjectConverter
                .convert(topLevelProject, source.session));
        fillContainer(container, projectByDirectoryMap, source.session);

        return container;
    }

    private static Map<File, MavenProject> getMapForProjects(MavenExecutionResult executionResult) {
        if (executionResult.getTopologicallySortedProjects().size() > 128) {
            return new TreeMap<>();
        } else {
            return new HashMap<>(executionResult.getTopologicallySortedProjects().size() + 1);
        }
    }

    private static void fillContainer(MavenProjectContainer rootContainer,
                                      Map<File, MavenProject> projectByDirectoryMap,
                                      MavenSession session) {
        ru.rzn.gmyasoedov.serverapi.model.MavenProject project = rootContainer.getProject();
        for (String module : project.getModulesDir()) {
            if (StringUtils.isEmpty(module)) continue;

            File moduleFile = new File(module);
            MavenProject mavenProjectByModuleFile = projectByDirectoryMap.get(moduleFile);
            MavenProjectContainer projectContainer = new MavenProjectContainer(
                    MavenProjectConverter.convert(mavenProjectByModuleFile, session)
            );
            rootContainer.getModules().add(projectContainer);
            fillContainer(projectContainer, projectByDirectoryMap, session);
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
