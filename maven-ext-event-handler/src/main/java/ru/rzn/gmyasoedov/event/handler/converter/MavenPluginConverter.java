package ru.rzn.gmyasoedov.event.handler.converter;

import org.apache.maven.model.Plugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;

public class MavenPluginConverter {
    public static MavenPlugin convert(Plugin plugin) {
        return new MavenPlugin(plugin.getGroupId(),
                plugin.getArtifactId(),
                plugin.getVersion());
    }
}
