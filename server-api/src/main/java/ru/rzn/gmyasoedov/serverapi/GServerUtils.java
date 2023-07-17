package ru.rzn.gmyasoedov.serverapi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.serverapi.model.MavenException;
import ru.rzn.gmyasoedov.serverapi.model.MavenResult;

import java.util.Collections;
import java.util.List;

import static ru.rzn.gmyasoedov.serverapi.GMavenServer.SERVER_ERROR_MESSAGE;

public abstract class GServerUtils {
    @NotNull
    public static MavenResult toResult(@NotNull Exception e) {
        List<MavenException> exceptions = Collections.singletonList(new MavenException(e.getMessage(), null, null));
        return new MavenResult(false, null, null, exceptions);
    }

    @NotNull
    public static MavenResult toResult(@Nullable MavenResult result) {
        if (result != null) return result;
        List<MavenException> exceptions = Collections
                .singletonList(new MavenException(SERVER_ERROR_MESSAGE, null, null));
        return new MavenResult(false, null, null, exceptions);
    }
}
