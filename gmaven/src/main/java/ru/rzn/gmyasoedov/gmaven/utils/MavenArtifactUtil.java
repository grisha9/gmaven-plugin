// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.utils;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.plugins.MavenPluginDescription;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MavenArtifactUtil {
    public static final String[] DEFAULT_GROUPS = new String[]{"org.apache.maven.plugins", "org.codehaus.mojo"};
    public static final String MAVEN_PLUGIN_DESCRIPTOR = "META-INF/maven/plugin.xml";


    public static boolean isPluginIdEquals(@Nullable String groupId1, @Nullable String artifactId1,
                                           @Nullable String groupId2, @Nullable String artifactId2) {
        if (artifactId1 == null) return false;

        if (!artifactId1.equals(artifactId2)) return false;

        if (groupId1 != null) {
            for (String group : DEFAULT_GROUPS) {
                if (groupId1.equals(group)) {
                    groupId1 = null;
                    break;
                }
            }
        }

        if (groupId2 != null) {
            for (String group : DEFAULT_GROUPS) {
                if (groupId2.equals(group)) {
                    groupId2 = null;
                    break;
                }
            }
        }

        return Objects.equals(groupId1, groupId2);
    }

    @Nullable
    public static MavenPluginDescription readPluginDescriptor(Path localRepository, MavenPlugin plugin) {
        Path path = getArtifactNioPath(localRepository, plugin.getGroupId(),
                plugin.getArtifactId(), plugin.getVersion(), "jar");
        return getPluginDescriptor(path);
    }

    @NotNull
    public static Path getArtifactNioPath(Path localRepository, String groupId, String artifactId,
                                          String version, String type) {
        Path dir = getArtifactDirectory(localRepository, groupId, artifactId);
        if (StringUtil.isEmpty(version)) version = resolveVersion(dir);
        return dir.resolve(version).resolve(artifactId + "-" + version + "." + type);
    }

    private static Path getArtifactDirectory(Path localRepository,
                                             String groupId,
                                             String artifactId) {
        String relativePath = StringUtil.replace(groupId, ".", File.separator) + File.separator + artifactId;
        return localRepository.resolve(relativePath);
    }

    private static String resolveVersion(Path pluginDir) {
        List<String> versions = new ArrayList<>();
        try (Stream<Path> children = Files.list(pluginDir)) {
            children.forEach(path -> {
                if (Files.isDirectory(path)) {
                    versions.add(path.getFileName().toString());
                }
            });
        } catch (NoSuchFileException e) {
            return "";
        } catch (Exception e) {
            MavenLog.LOG.warn(e.getMessage());
            return "";
        }

        if (versions.isEmpty()) return "";

        Collections.sort(versions);
        return versions.get(versions.size() - 1);
    }


    @Nullable
    private static MavenPluginDescription getPluginDescriptor(Path file) {
        try {
            if (!Files.exists(file)) return null;

            try (ZipFile jar = new ZipFile(file.toFile())) {
                ZipEntry entry = jar.getEntry(MAVEN_PLUGIN_DESCRIPTOR);

                if (entry == null) {
                    MavenLog.LOG.info("repository.plugin.corrupt " + file);
                    return null;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    byte[] bytes = FileUtil.loadBytes(is);
                    return new MavenPluginDescription(bytes);
                }
            }
        }
        catch (IOException e) {
            MavenLog.LOG.info(e);
            return null;
        }
    }
}
