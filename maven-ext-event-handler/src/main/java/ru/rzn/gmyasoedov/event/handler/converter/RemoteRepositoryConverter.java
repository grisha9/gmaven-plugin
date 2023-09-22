package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.artifact.repository.ArtifactRepository;
import ru.rzn.gmyasoedov.serverapi.model.MavenRemoteRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoteRepositoryConverter {

    public static List<MavenRemoteRepository> convert(List<ArtifactRepository> remoteRepositories) {
        if (remoteRepositories == null || remoteRepositories.isEmpty()) return Collections.emptyList();
        ArrayList<MavenRemoteRepository> result = new ArrayList<>(remoteRepositories.size());
        for (ArtifactRepository repository : remoteRepositories) {
            result.add(new MavenRemoteRepository(repository.getId(), repository.getUrl()));
        }
        return result;
    }
}
