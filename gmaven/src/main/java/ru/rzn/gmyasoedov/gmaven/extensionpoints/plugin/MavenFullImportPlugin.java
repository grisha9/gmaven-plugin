package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.openapi.extensions.ExtensionPointName;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import javax.annotation.Nonnull;

public interface MavenFullImportPlugin {
    ExtensionPointName<ApacheMavenCompilerPlugin> EP_NAME =
            ExtensionPointName.create("ru.rzn.gmyasoedov.gmaven.import.full.plugin");

    @Nonnull
    String getGroupId();

    @Nonnull
    String getArtifactId();

    @Nonnull
    default CompilerData getCompilerData(@Nonnull MavenProject project, @Nonnull MavenPlugin plugin) {
        throw new UnsupportedOperationException();
    };

    default String getKey() {
        return getGroupId() + ":" + getArtifactId();
    }

    default boolean isApplicable(@Nonnull MavenPlugin plugin) {
        return getArtifactId().equals(plugin.getArtifactId()) && getGroupId().equals(plugin.getGroupId());
    }

}
