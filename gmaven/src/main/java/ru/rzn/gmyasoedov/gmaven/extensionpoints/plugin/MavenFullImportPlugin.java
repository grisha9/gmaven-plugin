package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenContentRoot;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

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

    @NotNull
    default List<MavenContentRoot> getContentRoots(MavenPlugin plugin) {
        return Collections.emptyList();
    }
}
