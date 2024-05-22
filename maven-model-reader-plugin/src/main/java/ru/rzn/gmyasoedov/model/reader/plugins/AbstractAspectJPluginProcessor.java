package ru.rzn.gmyasoedov.model.reader.plugins;


import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import ru.rzn.gmyasoedov.model.reader.utils.MavenJDOMUtil;
import ru.rzn.gmyasoedov.model.reader.utils.PluginUtils;

import java.nio.file.Paths;

public abstract class AbstractAspectJPluginProcessor {

    public static void process(MavenProject project, Plugin plugin) {
        Xpp3Dom configuration = PluginUtils.getConfiguration(plugin, "compile");
        String srcPath = MavenJDOMUtil.findChildValueByPath(
                configuration, "aspectDirectory", Paths.get("src", "main", "aspect").toString()
        );
        Xpp3Dom testConfiguration = PluginUtils.getConfiguration(plugin, "test-compile");
        String testPath = MavenJDOMUtil.findChildValueByPath(
                testConfiguration, "testAspectDirectory", Paths.get("src", "test", "aspect").toString()
        );
        if (!srcPath.isEmpty()) {
            project.addCompileSourceRoot(srcPath);
        }
        if (!testPath.isEmpty()) {
            project.addTestCompileSourceRoot(testPath);
        }
    }
}
