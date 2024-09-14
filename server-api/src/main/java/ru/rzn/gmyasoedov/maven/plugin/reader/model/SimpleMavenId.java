package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import java.io.Serializable;
import java.util.Objects;

public class SimpleMavenId implements MavenId, Serializable {

    private String artifactId;
    private String groupId;
    private String version;

    public SimpleMavenId() {
    }

    public SimpleMavenId(String groupId, String artifactId, String version) {
        this.artifactId = ObjectUtils.defaultIfNull(artifactId, MavenId.UNKNOWN_VALUE);
        this.groupId = ObjectUtils.defaultIfNull(groupId, MavenId.UNKNOWN_VALUE);
        this.version = ObjectUtils.defaultIfNull(version, MavenId.UNKNOWN_VALUE);
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleMavenId that = (SimpleMavenId) o;

        if (!Objects.equals(artifactId, that.artifactId)) return false;
        if (!Objects.equals(groupId, that.groupId)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = artifactId != null ? artifactId.hashCode() : 0;
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MavenArtifact{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
