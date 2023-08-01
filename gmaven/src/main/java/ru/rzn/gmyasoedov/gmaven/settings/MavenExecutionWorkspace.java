package ru.rzn.gmyasoedov.gmaven.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MavenExecutionWorkspace implements Serializable {
    private static final long serialVersionUID = 2L;

    @NotNull
    private final List<ProfileExecution> profilesData = new ArrayList<>();
    @NotNull
    private final List<ProjectExecution> projectData = new ArrayList<>(2);
    @Nullable
    private String projectBuildFile;
    @Nullable
    private String subProjectBuildFile;

    public void addProfile(@Nullable ProfileExecution data) {
        if (data != null) {
            profilesData.add(data);
        }
    }

    @NotNull
    public List<ProfileExecution> getProfilesData() {
        return List.copyOf(profilesData);
    }

    public void addProject(@Nullable ProjectExecution data) {
        if (data != null) {
            projectData.add(data);
        }
    }

    @NotNull
    public List<ProjectExecution> getProjectData() {
        return List.copyOf(projectData);
    }

    @Nullable
    public String getProjectBuildFile() {
        return projectBuildFile;
    }

    @Nullable
    public void setProjectBuildFile(String projectBuildFile) {
        this.projectBuildFile = projectBuildFile;
    }

    public String getSubProjectBuildFile() {
        return subProjectBuildFile;
    }

    public void setSubProjectBuildFile(String subProjectBuildFile) {
        this.subProjectBuildFile = subProjectBuildFile;
    }
}
