package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MavenProjectContainer implements Serializable {
    private final MavenProject project;
    private final List<MavenProjectContainer> modules;

    public MavenProjectContainer() {
        this.project = null;
        this.modules = Collections.emptyList();
    }

    public MavenProjectContainer(MavenProject project) {
        this.project = project;
        this.modules = new ArrayList<>(project.getModulesDir().size());
    }

    public MavenProject getProject() {
        return project;
    }

    public List<MavenProjectContainer> getModules() {
        return modules;
    }

    @Override
    public String toString() {
        return project.toString();
    }
}
