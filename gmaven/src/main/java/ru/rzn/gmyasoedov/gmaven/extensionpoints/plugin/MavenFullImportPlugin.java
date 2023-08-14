package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.util.Collections;

public interface MavenFullImportPlugin {
    ExtensionPointName<MavenFullImportPlugin> EP_NAME =
            ExtensionPointName.create("ru.rzn.gmyasoedov.gmaven.import.full.plugin");

    @NotNull
    String getGroupId();

    @NotNull
    String getArtifactId();

    default String getKey() {
        return getGroupId() + ":" + getArtifactId();
    }

    default boolean isApplicable(@NotNull MavenPlugin plugin) {
        return getArtifactId().equals(plugin.getArtifactId()) && getGroupId().equals(plugin.getGroupId());
    }

    @NotNull
    default PluginContentRoots getContentRoots(MavenProject mavenProject, MavenPlugin plugin) {
        return new PluginContentRoots(Collections.emptyList(), Collections.emptySet());
    }
}
