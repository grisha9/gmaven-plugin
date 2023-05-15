package ru.rzn.gmyasoedov.serverapi.model;

import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@RequiredArgsConstructor
public class MavenProfile implements Serializable, Comparable<MavenProfile> {
    private final String name;
    private final boolean activation;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenProfile that = (MavenProfile) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(MavenProfile o) {
        return name.compareTo(o.name);
    }
}
