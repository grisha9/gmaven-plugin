package ru.rzn.gmyasoedov.maven.plugin.reader.model;


import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public final class PluginExecution implements Serializable {
    private String id;
    private String phase;
    private String configuration;
    private List<String> goals = Collections.emptyList();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getGoals() {
        return goals;
    }

    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return "PluginExecution{" +
                "id='" + id + '\'' +
                ", phase='" + phase + '\'' +
                ", goals=" + goals +
                ", configuration='" + configuration + '\'' +
                '}';
    }
}
