package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.serialization.PropertyMapping;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.profile.ProfileState;

public class ProfileData extends AbstractExternalEntityData implements Comparable<ProfileData> {
    @NotNull public static final Key<ProfileData> KEY = Key.create(ProfileData.class, 90);

    @NotNull
    private final String projectName;
    @NotNull
    private final String name;

    private final boolean hasActivation;

    @PropertyMapping({"owner", "projectName", "name", "hasActivation"})
    public ProfileData(@NotNull ProjectSystemId owner, @NotNull String projectName,
                       @NotNull String name, boolean hasActivation) {
        super(owner);
        this.projectName = projectName;
        this.name = name;
        this.hasActivation = hasActivation;
    }

    @NotNull
    public String getProjectName() {
        return projectName;
    }

    @NotNull
    public @NlsSafe String getName() {
        return name;
    }

    public boolean isHasActivation() {
        return hasActivation;
    }

    @Transient
    @NotNull
    public static ProfileState defaultState(ProfileData data) {
        ProfileState state = new ProfileState();
        if (data.hasActivation) {
            state.setActivationProfile(ProfileData.ActivationProfile.INDETERMINATE);
        } else {
            state.setSimpleProfile(SimpleProfile.INACTIVE);
        }
        return state;
    }

    @Transient
    @NotNull
    public String getStateKey() {
        return projectName + ":" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProfileData that = (ProfileData) o;

        if (hasActivation != that.hasActivation) return false;
        if (!projectName.equals(that.projectName)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + projectName.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (hasActivation ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull ProfileData that) {
        return name.compareTo(that.getName());
    }

    @Override
    public @NlsSafe String toString() {
        return name;
    }

    public enum SimpleProfile {ACTIVE, INACTIVE}
    public enum ActivationProfile {ACTIVE, INDETERMINATE, INACTIVE}
}