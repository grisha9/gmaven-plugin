package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.openapi.extensions.ExtensionPointName;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;

import javax.annotation.Nonnull;

public interface MavenFullImportPlugin {
    ExtensionPointName<MavenFullImportPlugin> EP_NAME =
            ExtensionPointName.create("ru.rzn.gmyasoedov.gmaven.import.full.plugin");

    @Nonnull
    String getGroupId();

    @Nonnull
    String getArtifactId();

    default String getKey() {
        return getGroupId() + ":" + getArtifactId();
    }

    default boolean isApplicable(@Nonnull MavenPlugin plugin) {
        return getArtifactId().equals(plugin.getArtifactId()) && getGroupId().equals(plugin.getGroupId());
    }

}
