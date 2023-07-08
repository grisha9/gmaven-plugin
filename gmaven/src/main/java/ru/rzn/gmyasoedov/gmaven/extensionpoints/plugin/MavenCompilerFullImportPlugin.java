package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import org.jdom.Element;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;

public interface MavenCompilerFullImportPlugin extends MavenFullImportPlugin {

    @Nonnull
    CompilerData getCompilerData(@Nonnull MavenProject project,
                                 @Nonnull MavenPlugin plugin,
                                 @Nonnull Path localRepositoryPath,
                                 @Nonnull Map<String, Element> contextElementMap);

    @Nullable
    String getAnnotationProcessorTagName();

}
