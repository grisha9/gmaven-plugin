package ru.rzn.gmyasoedov.serverapi.model.request;

import java.io.Serializable;
import java.util.List;

public class GetModelRequest implements Serializable {
    public String projectPath;
    public String dependencyAnalyzerGA;
    public String alternativePom;
    public String gMavenPluginPath;
    public List<String> tasks;
    public List<String> additionalArguments;
    public List<String> importArguments;
    public String projectList;
    public String profiles;
    public String threadCount;
    public boolean offline;
    public boolean nonRecursion;
    public boolean updateSnapshots;
    public boolean notUpdateSnapshots;
    public boolean quiteLogs;
    public boolean debugLog;
}
