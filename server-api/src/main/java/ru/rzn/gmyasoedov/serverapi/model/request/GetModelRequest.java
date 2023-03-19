package ru.rzn.gmyasoedov.serverapi.model.request;

import java.io.Serializable;

public class GetModelRequest implements Serializable {
    public String projectPath;
    public String artifactId;
    public String alternativePom;
    public String gMavenPluginPath;
    public boolean offline;
    public boolean nonRecursion;
}
