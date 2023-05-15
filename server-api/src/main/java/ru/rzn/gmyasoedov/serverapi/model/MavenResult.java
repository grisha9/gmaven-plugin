package ru.rzn.gmyasoedov.serverapi.model;

import java.io.Serializable;
import java.util.List;

public class MavenResult implements Serializable {
    public final boolean pluginNotResolved;
    public final MavenSettings settings;
    public final MavenProjectContainer projectContainer;
    public final List<MavenException> exceptions;

    public MavenResult(boolean pluginNotResolved,
                       MavenSettings settings,
                       MavenProjectContainer projectContainer,
                       List<MavenException> exceptions) {
        this.pluginNotResolved = pluginNotResolved;
        this.settings = settings;
        this.projectContainer = projectContainer;
        this.exceptions = exceptions;
    }
}
