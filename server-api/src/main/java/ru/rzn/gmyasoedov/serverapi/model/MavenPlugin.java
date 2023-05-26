
package ru.rzn.gmyasoedov.serverapi.model;

public final class MavenPlugin extends MavenId {
    static final long serialVersionUID = -6113607480882347420L;
    private final PluginBody body;

    public MavenPlugin(String groupId,
                       String artifactId,
                       String version,
                       PluginBody body) {
        super(groupId, artifactId, version);
        this.body = body;
    }

    public PluginBody getBody() {
        return body;
    }
}
