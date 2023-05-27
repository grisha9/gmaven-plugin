package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

public class DependencyAnalyzerData extends AbstractExternalEntityData implements Comparable<DependencyAnalyzerData> {
    @NotNull public static final Key<DependencyAnalyzerData> KEY = Key.create(DependencyAnalyzerData.class, 300);
    @NotNull
    private final String name;

    @PropertyMapping({"owner", "name"})
    public DependencyAnalyzerData(@NotNull ProjectSystemId owner, @NotNull String name) {
        super(owner);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(@NotNull DependencyAnalyzerData that) {
        return name.compareTo(that.getName());
    }

    @Override
    public @NlsSafe String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DependencyAnalyzerData that = (DependencyAnalyzerData) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}