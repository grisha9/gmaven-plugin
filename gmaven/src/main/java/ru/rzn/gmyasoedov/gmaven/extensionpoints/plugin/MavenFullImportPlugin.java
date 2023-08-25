package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.PluginContentRoots;
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil;
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
    default PluginContentRoots getContentRoots(@NotNull MavenProject mavenProject,
                                               @NotNull MavenPlugin plugin,
                                               @NotNull MavenProjectResolver.ProjectResolverContext context) {
        return new PluginContentRoots(Collections.emptyList(), Collections.emptySet());
    }

    @NotNull
    static Element parseConfiguration(@Nullable String configuration,
                                      @NotNull MavenProjectResolver.ProjectResolverContext context) {
        if (configuration == null) return MavenJDOMUtil.JDOM_ELEMENT_EMPTY;
        Element element = context.getContextElementMap().get(configuration);
        if (element != null) return element;
        element = MavenJDOMUtil.parseConfiguration(configuration);
        context.getContextElementMap().put(configuration, element);
        return element;
    }
}
