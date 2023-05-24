package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginData extends AbstractExternalEntityData implements ExternalConfigPathAware, Comparable<TaskData> {
    @NotNull public static final Key<PluginData> KEY = Key.create(PluginData.class, 251);

    @NotNull
    private final String name;
    @Nullable
    private final String description;
    @NotNull
    private final String linkedExternalProjectPath;
    @Nullable
    private String group;

    @PropertyMapping({"owner", "name", "linkedExternalProjectPath", "description", "group"})
    public PluginData(@NotNull ProjectSystemId owner,
                      @NotNull String name,
                      @NotNull String linkedExternalProjectPath,
                      @Nullable String description,
                      @Nullable String group) {
        super(owner);
        this.name = name;
        this.linkedExternalProjectPath = linkedExternalProjectPath;
        this.description = description;
        this.group = group;
    }

    @NotNull
    public @NlsSafe String getName() {
        return name;
    }

    @Override
    @NotNull
    public String getLinkedExternalProjectPath() {
        return linkedExternalProjectPath;
    }

    @Nullable
    public @NlsSafe String getDescription() {
        return description;
    }

    @Nullable
    public String getGroup() {
        return group;
    }

    public void setGroup(@Nullable String group) {
        this.group = group;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + linkedExternalProjectPath.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PluginData data = (PluginData) o;

        if (!name.equals(data.name)) return false;
        if (group != null ? !group.equals(data.group) : data.group != null) return false;
        if (!linkedExternalProjectPath.equals(data.linkedExternalProjectPath)) return false;
        if (description != null ? !description.equals(data.description) : data.description != null) return false;

        return true;
    }

    @Override
    public int compareTo(@NotNull TaskData that) {
        return name.compareTo(that.getName());
    }

    @Override
    public @NlsSafe String toString() {
        return name;
    }
}
