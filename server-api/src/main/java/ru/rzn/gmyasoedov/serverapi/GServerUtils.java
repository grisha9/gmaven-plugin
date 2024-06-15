package ru.rzn.gmyasoedov.serverapi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenException;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenId;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenMapResult;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProject;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static ru.rzn.gmyasoedov.serverapi.GMavenServer.SERVER_ERROR_MESSAGE;

public abstract class GServerUtils {
    @NotNull
    public static MavenMapResult toResult(@NotNull Exception e) {
        MavenException exception = new MavenException();
        exception.setMessage(e.getMessage());
        List<MavenException> exceptions = Collections.singletonList(exception);
        return toMapResult(false, exceptions);
    }

    @NotNull
    public static MavenMapResult toResult(@Nullable MavenMapResult result) {
        if (result != null) return result;
        MavenException exception = new MavenException();
        exception.setMessage(SERVER_ERROR_MESSAGE);
        List<MavenException> exceptions = Collections.singletonList(exception);
        return toMapResult(false, exceptions);
    }

    @NotNull
    public static String getMavenId(MavenId id) {
        return id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion();
    }

    public static @NotNull String getDisplayName(MavenProject project) {
        return (project.getName() == null || project.getName().isEmpty()) ? project.getArtifactId() : project.getName();
    }

    @Nullable
    public static String getFilePath(File file) {
        return file != null ? file.getAbsolutePath() : null;
    }

    private static MavenMapResult toMapResult(boolean pluginNotResolved, List<MavenException> exceptions) {
        MavenMapResult result = new MavenMapResult();
        result.pluginNotResolved = pluginNotResolved;
        result.exceptions = exceptions;
        return result;
    }
}
