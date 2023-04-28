package ru.rzn.gmyasoedov.serverapi.model;

import java.io.Serializable;
import java.util.List;

public class MavenResult implements Serializable {
    public final boolean pluginNotResolved;
    public final String localRepository;
    public final MavenProjectContainer projectContainer;
    public final List<MavenException> exceptions;

    public MavenResult(boolean pluginNotResolved,
                       String localRepository,
                       MavenProjectContainer projectContainer,
                       List<MavenException> exceptions) {
        this.pluginNotResolved = pluginNotResolved;
        this.localRepository = localRepository;
        this.projectContainer = projectContainer;
        this.exceptions = exceptions;
    }
}
