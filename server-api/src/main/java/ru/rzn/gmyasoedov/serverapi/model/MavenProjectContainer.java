package ru.rzn.gmyasoedov.serverapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MavenProjectContainer implements Serializable {
    private final boolean pluginNotResolved;
    private final MavenProject project;
    private final List<MavenProjectContainer> modules;

    public MavenProjectContainer(boolean pluginNotResolved) {
        this.pluginNotResolved = pluginNotResolved;
        this.project = null;
        this.modules = Collections.emptyList();
    }

    public MavenProjectContainer(MavenProject project) {
        this.project = project;
        this.modules = new ArrayList<>(project.getModulesDir().size());
        this.pluginNotResolved = false;
    }

    public MavenProject getProject() {
        return project;
    }

    public List<MavenProjectContainer> getModules() {
        return modules;
    }

    public boolean isPluginNotResolved() {
        return pluginNotResolved;
    }

    @Override
    public String toString() {
        return project.toString();
    }
}
