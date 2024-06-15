package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BuildErrors implements Serializable {
    public boolean pluginNotResolved;
    public List<MavenException> exceptions = Collections.emptyList();

    public boolean isPluginNotResolved() {
        return pluginNotResolved;
    }

    public void setPluginNotResolved(boolean pluginNotResolved) {
        this.pluginNotResolved = pluginNotResolved;
    }

    public List<MavenException> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<MavenException> exceptions) {
        this.exceptions = exceptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildErrors that = (BuildErrors) o;
        return pluginNotResolved == that.pluginNotResolved && Objects.equals(exceptions, that.exceptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginNotResolved, exceptions);
    }
}
