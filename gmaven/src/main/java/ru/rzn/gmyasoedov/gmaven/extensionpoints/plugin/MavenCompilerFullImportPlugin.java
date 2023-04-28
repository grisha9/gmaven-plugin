package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public interface MavenCompilerFullImportPlugin extends MavenFullImportPlugin {

    @Nonnull
    CompilerData getCompilerData(@Nonnull MavenProject project,
                                 @Nonnull MavenPlugin plugin,
                                 @Nonnull Path localRepositoryPath);

}
