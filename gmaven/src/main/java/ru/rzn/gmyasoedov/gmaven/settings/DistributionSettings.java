package ru.rzn.gmyasoedov.gmaven.settings;

import java.nio.file.Path;
import java.util.Objects;

public class DistributionSettings {
    private DistributionType type;
    private Path path;
    private String url;

    public DistributionSettings(DistributionType type, Path path, String url) {
        this.type = Objects.requireNonNull(type);
        this.path = path;
        this.url = url;
    }

    public DistributionSettings() {
    }

    public DistributionType getType() {
        return type;
    }

    public void setType(DistributionType type) {
        this.type = Objects.requireNonNull(type);
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistributionSettings that = (DistributionSettings) o;

        if (type != that.type) return false;
        if (!Objects.equals(path, that.path)) return false;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
