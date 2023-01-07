package ru.rzn.gmyasoedov.serverapi.model.request;

import java.io.Serializable;

public class GetModelRequest implements Serializable {
    public final String projectPath;
    public final String artifactId;
    public final boolean offline;

    public GetModelRequest(String projectPath) {
        this.projectPath = projectPath;
        this.artifactId = null;
        this.offline = false;
    }
}
