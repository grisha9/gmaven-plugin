package ru.rzn.gmyasoedov.serverapi.model.request;

import java.io.Serializable;
import java.util.List;

public class GetModelRequest implements Serializable {
    public String projectPath;
    public String artifactId;
    public String analyzerGA;
    public String alternativePom;
    public String gMavenPluginPath;
    public List<String> tasks;
    public String profiles;
    public String threadCount;
    public boolean offline;
    public boolean nonRecursion;
    public boolean updateSnapshots;
    public boolean quiteLogs;
    public boolean debugLog;
}
