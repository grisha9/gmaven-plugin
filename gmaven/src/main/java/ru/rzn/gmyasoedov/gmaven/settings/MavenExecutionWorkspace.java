package ru.rzn.gmyasoedov.gmaven.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MavenExecutionWorkspace implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private final List<ProfileExecution> profilesData = new ArrayList<>();

    @Nullable
    private String artifactGA;

    public void addProfile(@Nullable ProfileExecution data) {
        if (data != null) {
            profilesData.add(data);
        }
    }

    @NotNull
    public List<ProfileExecution> getProfilesData() {
        return List.copyOf(profilesData);
    }

    @Nullable
    public String getArtifactGA() {
        return artifactGA;
    }

    public void setArtifactGA(@Nullable String artifactGA) {
        this.artifactGA = artifactGA;
    }
}
