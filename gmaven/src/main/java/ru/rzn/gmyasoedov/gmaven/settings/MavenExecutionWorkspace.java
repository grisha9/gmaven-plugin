package ru.rzn.gmyasoedov.gmaven.settings;

import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MavenExecutionWorkspace implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private final List<ProfileData> profilesData = new ArrayList<>();

    public void addProfile(ProfileData data) {
        profilesData.add(data);
    }

    public List<ProfileData> getProfilesData() {
        return List.copyOf(profilesData);
    }
}
