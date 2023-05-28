package ru.rzn.gmyasoedov.gmaven.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MavenExecutionWorkspace implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private final List<ProfileExecution> profilesData = new ArrayList<>();

    public void addProfile(@Nullable ProfileExecution data) {
        if (data != null) {
            profilesData.add(data);
        }
    }

    public List<ProfileExecution> getProfilesData() {
        return List.copyOf(profilesData);
    }
}
