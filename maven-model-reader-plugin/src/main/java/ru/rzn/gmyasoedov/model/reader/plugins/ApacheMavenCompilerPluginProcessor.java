package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import ru.rzn.gmyasoedov.model.reader.utils.GUtils;
import ru.rzn.gmyasoedov.model.reader.utils.MavenContextUtils;
import ru.rzn.gmyasoedov.model.reader.utils.MavenJDOMUtil;
import ru.rzn.gmyasoedov.model.reader.utils.PluginUtils;

import java.util.List;
import java.util.Objects;

import static ru.rzn.gmyasoedov.model.reader.utils.MavenContextUtils.GENERATED_PATH;
import static ru.rzn.gmyasoedov.model.reader.utils.MavenContextUtils.GENERATED_TEST_PATH;

public class ApacheMavenCompilerPluginProcessor implements PluginProcessor {

    @Override
    public String groupId() {
        return "org.apache.maven.plugins";
    }

    @Override
    public String artifactId() {
        return "maven-compiler-plugin";
    }

    @Override
    public void process(MavenProject project, Plugin plugin) {
        Xpp3Dom configuration = PluginUtils.getConfiguration(plugin, "compile");
        String srcPath = MavenJDOMUtil.findChildValueByPath(configuration, "generatedSourcesDirectory", "");

        Xpp3Dom testConfiguration = PluginUtils.getConfiguration(plugin, "test-compile");
        String testPath = MavenJDOMUtil.findChildValueByPath(testConfiguration, "generatedTestSourcesDirectory", "");
        if (!srcPath.isEmpty()) {
            MavenContextUtils.addStringValue(
                    project, GENERATED_PATH, GUtils.getAbsolutePath(srcPath, project.getBuild().getDirectory())
            );
        }
        if (!testPath.isEmpty()) {
            MavenContextUtils.addStringValue(
                    project, GENERATED_TEST_PATH, GUtils.getAbsolutePath(testPath, project.getBuild().getDirectory())
            );
        }
        applyGroovyProcessor(project, plugin);
    }

    private static void applyGroovyProcessor(MavenProject project, Plugin plugin) {
        List<Dependency> dependencies = plugin.getDependencies();
        if (dependencies == null || dependencies.isEmpty()) return;
        boolean isGroovy = false;
        for (Dependency each : dependencies) {
            if (Objects.equals(each.getGroupId(), "org.codehaus.groovy")
                    && Objects.equals(each.getArtifactId(), "groovy-eclipse-compiler")) {
                isGroovy = true;
            }
        }
        if (!isGroovy) return;
        AbstractGroovyPluginProcessor.process(project, plugin);
    }
}
