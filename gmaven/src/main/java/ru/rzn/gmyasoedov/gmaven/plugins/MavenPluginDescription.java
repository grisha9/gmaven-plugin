package ru.rzn.gmyasoedov.gmaven.plugins;

import org.jdom.Element;
import ru.rzn.gmyasoedov.gmaven.utils.MavenJDOMUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MavenPluginDescription {
    private static final String UNKNOWN = "<unknown>";

    private final String myGroupId;
    private final String myArtifactId;
    private final String myVersion;
    private final String myGoalPrefix;
    private final Map<String, Mojo> myMojos;
    private final Map<String, String> myParams = new HashMap<>();

    public MavenPluginDescription(Element plugin) {
        myGroupId = MavenJDOMUtil.findChildValueByPath(plugin, "groupId", "unknown");
        myArtifactId = MavenJDOMUtil.findChildValueByPath(plugin, "artifactId", "unknown");
        myVersion = MavenJDOMUtil.findChildValueByPath(plugin, "version", "unknown");
        myGoalPrefix = MavenJDOMUtil.findChildValueByPath(plugin, "goalPrefix", "unknown");
        myMojos = readMojos(plugin);
    }

    private Map<String, Mojo> readMojos(Element plugin) {
        Map<String, Mojo> result = new LinkedHashMap<>();
        for (Element each : MavenJDOMUtil.findChildrenByPath(plugin, "mojos", "mojo")) {
            Element configuration = each.getChild("configuration");
            if (configuration != null) {
                Element source = configuration.getChild("source");
                if (source != null) {
                    myParams.put("source", source.getAttributeValue("default-value"));
                }
                Element target = configuration.getChild("source");
                if (target != null) {
                    myParams.put("target", source.getAttributeValue("default-value"));
                }
            }
            String goal = MavenJDOMUtil.findChildValueByPath(each, "goal", "unknown");
            result.put(goal, new Mojo(goal));
        }
        return result;
    }

    public String getGroupId() {
        return myGroupId;
    }

    public String getArtifactId() {
        return myArtifactId;
    }

    public String getVersion() {
        return myVersion;
    }

    public String getGoalPrefix() {
        return myGoalPrefix;
    }

    public Collection<Mojo> getMojos() {
        return myMojos.values();
    }

    public Mojo findMojo(String name) {
        return myMojos.get(name);
    }

    public Map<String, String> getMyParams() {
        return myParams;
    }

    public final class Mojo {
        private final String myGoal;

        private Mojo(String goal) {
            myGoal = goal;
        }

        public String getGoal() {
            return myGoal;
        }

        public String getDisplayName() {
            return myGoalPrefix + ":" + myGoal;
        }

        public String getQualifiedGoal() {
            StringBuilder builder = new StringBuilder();
            append(builder, myGroupId);
            append(builder, myArtifactId);
            append(builder, myVersion);
            append(builder, myGoal);

            return builder.toString();
        }
    }

    private static void append(StringBuilder builder, String part) {
        if (builder.length() != 0) builder.append(':');
        builder.append(part == null ? UNKNOWN : part);
    }
}
