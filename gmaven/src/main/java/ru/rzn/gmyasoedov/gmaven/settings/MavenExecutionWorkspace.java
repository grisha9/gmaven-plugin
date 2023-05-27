package ru.rzn.gmyasoedov.gmaven.settings;

import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.profile.ProfileState;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class MavenExecutionWorkspace implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private Map<String, ProfileState> profilesState = Collections.emptyMap();

    public Map<String, ProfileState> getProfilesState() {
        return Collections.unmodifiableMap(profilesState);
    }

    public void setProfilesState(@NotNull Map<String, ProfileState> profilesState) {
        this.profilesState = profilesState;
    }
}
