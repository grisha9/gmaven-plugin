
package ru.rzn.gmyasoedov.event.handler.model;


import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenId;

public final class MavenPluginWrapper implements MavenId {
    private final PluginBodyWrapper body;
    private final String groupId;
    private final String artifactId;
    private final String version;

    public MavenPluginWrapper(String groupId,
                              String artifactId,
                              String version,
                              PluginBodyWrapper body) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.body = body;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public PluginBodyWrapper getBody() {
        return body;
    }
}
