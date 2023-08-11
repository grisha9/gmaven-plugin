package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class ProfileExecution implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull
    private final String name;
    private boolean enabled;

    public ProfileExecution(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Transient
    public String toRawName() {
        return enabled ? name : "!" + name;
    }
}