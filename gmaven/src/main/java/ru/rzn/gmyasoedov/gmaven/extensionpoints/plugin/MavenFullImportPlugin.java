package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver;
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil;
import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Nullable
    static String getAbsoluteContentPath(@NotNull String sourcePath, @NotNull MavenProject mavenProject) {
        try {
            var path = Paths.get(sourcePath);
            if (path.isAbsolute()) {
                return sourcePath;
            } else {
                return Path.of(mavenProject.getBasedir(), sourcePath).toAbsolutePath().toString();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
