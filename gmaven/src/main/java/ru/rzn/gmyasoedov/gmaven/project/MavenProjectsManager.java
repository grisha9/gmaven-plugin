package ru.rzn.gmyasoedov.gmaven.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class MavenProjectsManager {

    public static MavenProjectsManager getInstance(@NotNull Project project) {
        return project.getService(MavenProjectsManager.class);
    }

    public List<MavenProject> getProjects() {
        return Collections.emptyList();
    }

    public MavenProject findProject(VirtualFile file) {
        return null;
    }

    public File getLocalRepository() {
        return null;
    }

    public MavenProject[] findInheritors(MavenProject mavenProject) {
        return new MavenProject[0];
    }

    public MavenProject findProject(MavenId id) {
        return null;
    }

    public MavenProject findContainingProject(VirtualFile file) {
        return null;
    }
}
