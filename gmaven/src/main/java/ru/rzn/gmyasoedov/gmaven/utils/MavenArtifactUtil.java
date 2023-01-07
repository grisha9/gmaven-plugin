// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.utils;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    /**
     * @deprecated use {@link #getArtifactNioPath(File, String, String, String, String)} instead
     */
    @Deprecated
    @NotNull
    public static File getArtifactFile(File localRepository, String groupId, String artifactId, String version, String type) {
        return getArtifactNioPath(localRepository, groupId, artifactId, version, type).toFile();
    }

    @NotNull
    public static Path getArtifactNioPath(File localRepository, String groupId, String artifactId, String version, String type) {
        Path dir = null;
        if (StringUtil.isEmpty(groupId)) {
            for (String each : DEFAULT_GROUPS) {
                dir = getArtifactDirectory(localRepository, each, artifactId);
                if (Files.exists(dir)) break;
            }
        } else {
            dir = getArtifactDirectory(localRepository, groupId, artifactId);
        }

        if (StringUtil.isEmpty(version)) version = resolveVersion(dir);
        return dir.resolve(version).resolve(artifactId + "-" + version + "." + type);
    }

    private static Path getArtifactDirectory(File localRepository,
                                             String groupId,
                                             String artifactId) {
        String relativePath = StringUtil.replace(groupId, ".", File.separator) + File.separator + artifactId;
        return localRepository.toPath().resolve(relativePath);
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


}
