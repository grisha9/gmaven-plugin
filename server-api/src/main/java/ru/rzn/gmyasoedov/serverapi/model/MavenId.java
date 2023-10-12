package ru.rzn.gmyasoedov.serverapi.model;

import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

@SuperBuilder
public class MavenId implements Serializable {
    private static final long serialVersionUID = -6113607480882347420L;
    public static final String UNKNOWN_VALUE = "Unknown";

    @NotNull
    protected final String groupId;
    @NotNull
    protected final String artifactId;
    @NotNull
    protected final String version;

    private String id;

    public MavenId(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
        this.groupId = ObjectUtils.defaultIfNull(groupId, UNKNOWN_VALUE);
        this.artifactId = ObjectUtils.defaultIfNull(artifactId, UNKNOWN_VALUE);
        this.version = ObjectUtils.defaultIfNull(version, UNKNOWN_VALUE);
    }

    public MavenId(@Nullable String coord) {
        if (coord == null) {
            groupId = artifactId = version = UNKNOWN_VALUE;
        } else {
            String[] parts = coord.split(":");
            groupId = parts.length > 0 ? parts[0] : UNKNOWN_VALUE;
            artifactId = parts.length > 1 ? parts[1] : UNKNOWN_VALUE;
            version = parts.length > 2 ? parts[2] : UNKNOWN_VALUE;
        }
    }

    @NotNull
    public String getGroupId() {
        return groupId;
    }

    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    @NotNull
    public String getVersion() {
        return version;
    }

    @NotNull
    public String getId() {
        if (id != null) return id;
        return id = groupId + ":" + artifactId  + ":" + version;
    }

    @NotNull
    public String getDisplayString() {
        return getId();
    }

    @Override
    public String toString() {
        return getDisplayString();
    }

    public boolean equals(@Nullable String groupId, @Nullable String artifactId) {
        if (!Objects.equals(this.artifactId, artifactId)) return false;
        if (!Objects.equals(this.groupId, groupId)) return false;
        return true;
    }

    public boolean equals(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
        if (!equals(groupId, artifactId)) return false;
        if (!Objects.equals(this.version, version)) return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenId other = (MavenId) o;
        return equals(other.getGroupId(), other.artifactId, other.version);
    }

    @Override
    public int hashCode() {
        int result;
        result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
