package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.serialization.PropertyMapping;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProfileData extends AbstractExternalEntityData implements Comparable<ProfileData> {
    @NotNull public static final Key<ProfileData> KEY = Key.create(ProfileData.class, 90);

    @NotNull
    private final String name;
    @Nullable
    private SimpleProfile simpleProfile;
    @Nullable
    private ActivationProfile activationProfile;

    @PropertyMapping({"owner", "name", "simpleProfile", "activationProfile"})
    public ProfileData(@NotNull ProjectSystemId owner, @NotNull String name,
                       @Nullable SimpleProfile simpleProfile, @Nullable ActivationProfile activationProfile) {
        super(owner);
        this.name = name;
        this.simpleProfile = simpleProfile;
        this.activationProfile = activationProfile;
    }

    @NotNull
    public @NlsSafe String getName() {
        return name;
    }

    @Nullable
    public SimpleProfile getSimpleProfile() {
        return simpleProfile;
    }

    @Nullable
    public ActivationProfile getActivationProfile() {
        return activationProfile;
    }

    @Transient
    @NotNull
    public String getState() {
        if (simpleProfile != null) return simpleProfile.name();
        if (activationProfile != null) return activationProfile.name();
        throw new IllegalStateException("profile type is null " + name);
    }

    @Transient
    public void nextState() {
        if (simpleProfile != null) {
            simpleProfile = SimpleProfile.values()[(simpleProfile.ordinal() + 1) % SimpleProfile.values().length];
        }
        if (activationProfile != null) {
            activationProfile = ActivationProfile
                    .values()[(activationProfile.ordinal() + 1) % ActivationProfile.values().length];
        }
    }

    @Transient
    public boolean hasActivation() {
        return activationProfile != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProfileData that = (ProfileData) o;

        if (!name.equals(that.name)) return false;
        if (simpleProfile != that.simpleProfile) return false;
        return activationProfile == that.activationProfile;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (simpleProfile != null ? simpleProfile.hashCode() : 0);
        result = 31 * result + (activationProfile != null ? activationProfile.hashCode() : 0);
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