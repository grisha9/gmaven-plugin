package ru.rzn.gmyasoedov.maven.plugin.reader.model;


import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class PluginBody implements Serializable {
    private List<PluginExecution> executions = Collections.emptyList();
    private List<MavenArtifact> dependencies = Collections.emptyList();
    private String configuration;

    public List<PluginExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<PluginExecution> executions) {
        this.executions = executions;
    }

    public List<MavenArtifact> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<MavenArtifact> dependencies) {
        this.dependencies = dependencies;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return "PluginBody{" +
                "executions=" + executions +
                ", dependencies=" + dependencies +
                ", configuration='" + configuration + '\'' +
                '}';
    }
}
