package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.nio.file.Path;
import java.util.Map;

public interface MavenCompilerFullImportPlugin extends MavenFullImportPlugin {

    @NotNull
    CompilerData getCompilerData(@NotNull MavenProject project,
                                 @NotNull MavenPlugin plugin,
                                 @NotNull Path localRepositoryPath,
                                 @NotNull Map<String, Element> contextElementMap);

    @NotNull
    MainJavaCompilerData getJavaCompilerData(@NotNull MavenProject project,
                                             @NotNull MavenPlugin plugin,
                                             @NotNull CompilerData compilerData,
                                             @NotNull Path localRepositoryPath,
                                             @NotNull Map<String, Element> contextElementMap);


    @Nullable
    String getAnnotationProcessorTagName();

}
