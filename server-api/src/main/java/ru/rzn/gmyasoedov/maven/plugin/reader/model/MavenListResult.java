package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class MavenListResult implements Serializable {
    public boolean pluginNotResolved;
    public MavenSettings settings;
    public List<MavenProject> mavenProjects = Collections.emptyList();
    public List<String> exceptions = Collections.emptyList();
}
