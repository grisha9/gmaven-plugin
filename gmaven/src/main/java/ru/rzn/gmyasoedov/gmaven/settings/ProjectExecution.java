package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

public class ProjectExecution implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull
    private final String name;
    private final boolean enabled;

    public ProjectExecution(@NotNull String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Transient
    public String toRawName() {
        return enabled ? name : "!" + name;
    }
}