package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProfile;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenRemoteRepository;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenSettings;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MavenSettingsConverter {

    public static MavenSettings convert(EventSpyResultHolder source) {
        MavenSession session = source.session;
        if (session == null) {
            MavenSettings result = new MavenSettings();
            result.modulesCount = 0;
            return result;
        }
        String localRepository = session.getRequest().getLocalRepository().getBasedir();
        File settingsFile = session.getRequest().getUserSettingsFile();
        String settingsFilePath = settingsFile == null ? null : settingsFile.getAbsolutePath();
        Collection<String> activeProfiles = source.settingsActiveProfiles;
        Collection<MavenRemoteRepository> repositories = RemoteRepositoryConverter
                .convert(session.getRequest().getRemoteRepositories());
        MavenSettings result = new MavenSettings();
        result.setModulesCount(session.getAllProjects() == null ? 0 : session.getAllProjects().size());
        result.setLocalRepository(localRepository);
        result.setSettingsPath(settingsFilePath);
        result.setRemoteRepositories(repositories);
        result.setProfiles(getMavenProfiles(session, activeProfiles));
        return result;
    }

    private static Collection<MavenProfile> getMavenProfiles(MavenSession session,
                                                             Collection<String> activeProfiles) {
        Set<MavenProfile> profiles = new HashSet<>(session.getRequest().getProfiles().size() * 2);
        for (Profile profile : session.getRequest().getProfiles()) {
            profiles.add(createMavenProfile(profile, activeProfiles));
        }
        if (session.getProjects() != null) {
            for (MavenProject project : session.getProjects()) {
                for (Profile profile : project.getModel().getProfiles()) {
                    profiles.add(createMavenProfile(profile, activeProfiles));
                }
            }
        }
        return profiles;
    }

    private static MavenProfile createMavenProfile(Profile profile, Collection<String> activeProfiles) {
        MavenProfile result = new MavenProfile();
        result.setName(profile.getId());
        result.setActivation(profile.getActivation() != null || activeProfiles.contains(profile.getId()));
        return result;
    }
}
