package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder;
import ru.rzn.gmyasoedov.serverapi.model.MavenProfile;
import ru.rzn.gmyasoedov.serverapi.model.MavenSettings;

import java.io.File;
import java.util.*;

public class MavenSettingsConverter {

    public static MavenSettings convert(EventSpyResultHolder source) {
        MavenSession session = source.session;
        if (session == null) {
            return new MavenSettings(null, null, Collections.<MavenProfile>emptyList());
        }
        String localRepository = session.getRequest().getLocalRepository().getBasedir();
        File settingsFile = session.getRequest().getUserSettingsFile();
        String settingsFilePath = settingsFile == null ? null : settingsFile.getAbsolutePath();
        Collection<String> activeProfiles = getActiveProfiles(session);
        return new MavenSettings(
                localRepository, settingsFilePath, getMavenProfiles(session, activeProfiles)
        );
    }

    private static Collection<MavenProfile> getMavenProfiles(MavenSession session,
                                                             Collection<String> activeProfiles) {
        TreeSet<MavenProfile> profiles = new TreeSet<>();
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

    private static Map<File, MavenProject> getMapForProjects(MavenExecutionResult executionResult) {
        if (executionResult.getTopologicallySortedProjects().size() > 128) {
            return new TreeMap<>();
        } else {
            return new HashMap<>(executionResult.getTopologicallySortedProjects().size() + 1);
        }
    }
}
