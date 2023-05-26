package ru.rzn.gmyasoedov.serverapi.model;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Collection;

@RequiredArgsConstructor
public class MavenSettings implements Serializable {
    public final String localRepository;
    public final String settingsPath;
    @NonNull
    public final Collection<MavenProfile> profiles;
    @NonNull
    public final Collection<MavenRemoteRepository> remoteRepositories;
}
