package ru.rzn.gmyasoedov.maven.plugin.reader.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public class MavenSettings implements Serializable {
    public int modulesCount;
    public String localRepository;
    public String settingsPath;
    public Collection<MavenProfile> profiles = Collections.emptyList();
    public Collection<MavenRemoteRepository> remoteRepositories = Collections.emptyList();

    public int getModulesCount() {
        return modulesCount;
    }

    public void setModulesCount(int modulesCount) {
        this.modulesCount = modulesCount;
    }

    public String getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(String localRepository) {
        this.localRepository = localRepository;
    }

    public String getSettingsPath() {
        return settingsPath;
    }

    public void setSettingsPath(String settingsPath) {
        this.settingsPath = settingsPath;
    }

    public Collection<MavenProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Collection<MavenProfile> profiles) {
        this.profiles = profiles;
    }

    public Collection<MavenRemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(Collection<MavenRemoteRepository> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }
}
