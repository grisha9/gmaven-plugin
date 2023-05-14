package ru.rzn.gmyasoedov.serverapi.model.request;

import java.io.Serializable;
import java.util.List;

public class GetModelRequest implements Serializable {
    public String projectPath;
    public String artifactId;
    public String alternativePom;
    public String gMavenPluginPath;
    public List<String> tasks;
    public boolean offline;
    public boolean nonRecursion;
}
