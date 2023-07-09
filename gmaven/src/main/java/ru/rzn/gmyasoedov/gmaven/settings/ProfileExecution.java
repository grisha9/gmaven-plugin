package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

public class ProfileExecution implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull
    private String name;
    private boolean enabled;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Transient
    public String toRawName() {
        return enabled ? name : "-" + name;
    }
}