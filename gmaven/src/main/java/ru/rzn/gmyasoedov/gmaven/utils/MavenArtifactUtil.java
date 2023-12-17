package ru.rzn.gmyasoedov.gmaven.utils;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.plugins.MavenPluginDescription;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MavenArtifactUtil {
    public static final String GROUP_ID = "groupId";
    public static final String VERSION = "version";
    public static final String ARTIFACT_ID="artifactId";
    public static final String RELATIVE_PATH = "relativePath";
    public static final String PARENT = "parent";
    public static final String MODULE = "module";
    public static final String BUILD = "build";
    public static final String PROPERTIES = "properties";
    public static final String DEPENDENCY_MANAGEMENT = "dependencyManagement";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEPENDENCY = "dependency";
    public static final String EXCLUSION = "exclusion";
    public static final String PLUGIN_MANAGEMENT = "pluginManagement";
    public static final String PLUGINS = "plugins";
    public static final String PLUGIN = "plugin";
    public static final String MAVEN_PLUGIN_DESCRIPTOR = "META-INF/maven/plugin.xml";

    private static final Map<MavenId, MavenPluginDescription> PLUGIN_DESCRIPTOR_CACHE = new ConcurrentHashMap<>();

    public static void clearPluginDescriptorCache() {
        if (!PLUGIN_DESCRIPTOR_CACHE.isEmpty()) {
            PLUGIN_DESCRIPTOR_CACHE.clear();
        }
    }

    @Nullable
    public static MavenPluginDescription readPluginDescriptor(Path localRepository, MavenPlugin plugin) {
        MavenPluginDescription description = PLUGIN_DESCRIPTOR_CACHE.get(plugin);
        if (description != null) {
            return description;
        }
        Path path = getArtifactNioPath(localRepository, plugin.getGroupId(),
                plugin.getArtifactId(), plugin.getVersion(), "jar");
        MavenPluginDescription pluginDescriptor = getPluginDescriptor(path);
        if (pluginDescriptor != null) {
            PLUGIN_DESCRIPTOR_CACHE.putIfAbsent(plugin, pluginDescriptor);
        }
        return pluginDescriptor;
    }

    @NotNull
    public static Path getArtifactNioPathPom(@NotNull Path localRepository,
                                          @NotNull String groupId,
                                          @NotNull String artifactId,
                                          @NotNull String version) {
        return getArtifactNioPath(localRepository, groupId, artifactId, version, "pom", null);
    }

    @NotNull
    public static Path getArtifactNioPath(@NotNull Path localRepository,
                                          @NotNull String groupId,
                                          @NotNull String artifactId,
                                          @NotNull String version,
                                          @NotNull String extension) {
        return getArtifactNioPath(localRepository, groupId, artifactId, version, extension, null);
    }

    @NotNull
    public static Path getArtifactNioPath(@NotNull Path localRepository,
                                          @NotNull String groupId,
                                          @NotNull String artifactId,
                                          @NotNull String version,
                                          @NotNull String extension,
                                          @Nullable String classifier) {
        Path dir = getArtifactDirectory(localRepository, groupId, artifactId);
        if (StringUtil.isEmpty(version)) version = resolveVersion(dir);

        return dir.resolve(version).resolve(artifactId + "-" + version +
                (classifier == null ? "." + extension : "-" + classifier + "." + extension));
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
                    try {
                        Element pluginDescriptionElement = JDOMUtil.load(is);
                        return new MavenPluginDescription(pluginDescriptionElement);
                    } catch (Exception e) {
                        MavenLog.LOG.error("repository.plugin.corrupt " + file, e);
                        return null;
                    }
                }
            }
        } catch (IOException e) {
            MavenLog.LOG.info(e);
            return null;
        }
    }
}
