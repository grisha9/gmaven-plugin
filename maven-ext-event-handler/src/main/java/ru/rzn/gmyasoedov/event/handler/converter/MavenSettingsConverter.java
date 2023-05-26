package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.MavenProfile;
import ru.rzn.gmyasoedov.serverapi.model.MavenRemoteRepository;
import ru.rzn.gmyasoedov.serverapi.model.MavenSettings;

import java.io.File;
import java.util.*;

public class MavenSettingsConverter {

    public static MavenSettings convert(EventSpyResultHolder source) {
        MavenSession session = source.session;
        if (session == null) {
            return new MavenSettings(
                    null, null, Collections.<MavenProfile>emptyList(), Collections.<MavenRemoteRepository>emptyList()
            );
        }
        String localRepository = session.getRequest().getLocalRepository().getBasedir();
        File settingsFile = session.getRequest().getUserSettingsFile();
        String settingsFilePath = settingsFile == null ? null : settingsFile.getAbsolutePath();
        Collection<String> activeProfiles = getActiveProfiles(session);
        Collection<MavenRemoteRepository> repositories = getRemoteRepositories(session);
        return new MavenSettings(
                localRepository, settingsFilePath, getMavenProfiles(session, activeProfiles), repositories
        );
    }

    private static Collection<MavenRemoteRepository> getRemoteRepositories(MavenSession session) {
        List<ArtifactRepository> remoteRepositories = session.getRequest().getRemoteRepositories();
        if (remoteRepositories.isEmpty()) return Collections.emptyList();
        ArrayList<MavenRemoteRepository> result = new ArrayList<>(remoteRepositories.size());
        for (ArtifactRepository repository : remoteRepositories) {
            result.add(new MavenRemoteRepository(repository.getId(), repository.getUrl()));
        }
        return result;
    }

    private static Collection<MavenProfile> getMavenProfiles(MavenSession session,
                                                             Collection<String> activeProfiles) {
        Set<MavenProfile> profiles = new HashSet<>(session.getRequest().getProfiles().size() * 2);
        for (Profile profile : session.getRequest().getProfiles()) {
            profiles.add(createMavenProfile(profile, activeProfiles));
        }
        for (MavenProject project : session.getProjects()) {
            for (Profile profile : project.getModel().getProfiles()) {
                profiles.add(createMavenProfile(profile, activeProfiles));
            }
        }
        return profiles;
    }

    private static MavenProfile createMavenProfile(Profile profile, Collection<String> activeProfiles) {
        return new MavenProfile(
                profile.getId(), profile.getActivation() != null || activeProfiles.contains(profile.getId())
        );
    }

    private static Collection<String> getActiveProfiles(MavenSession session) {
        List<String> activeProfiles = session.getRequest().getActiveProfiles();
        if (activeProfiles.size() < 5) return activeProfiles;
        return new HashSet<>(activeProfiles);
    }
}
