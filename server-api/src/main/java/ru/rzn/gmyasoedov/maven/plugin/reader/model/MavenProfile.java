package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import java.io.Serializable;
import java.util.Objects;

public class MavenProfile implements Serializable {
    public String name;
    public boolean activation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActivation() {
        return activation;
    }

    public void setActivation(boolean activation) {
        this.activation = activation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenProfile that = (MavenProfile) o;
        return activation == that.activation && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, activation);
    }
}
