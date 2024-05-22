package ru.rzn.gmyasoedov.model.reader.plugins;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import java.util.HashMap;
import java.util.Map;

public class PluginProcessorManager {
    private static final Map<String, PluginProcessor> processors = new HashMap<>();

    static {
        addProcessor(new ApacheMavenCompilerPluginProcessor());
        addProcessor(new BuildHelperMavenPluginProcessor());
        addProcessor(new CodehausAspectjMavenPlugin());
        addProcessor(new DevAspectjMavenPlugin());
        addProcessor(new GroovyMavenPlusPlugin());
        addProcessor(new GroovyMavenPlugin());
        addProcessor(new KotlinMavenPluginProcessor());
    }

    private static void addProcessor(PluginProcessor processor) {
        processors.put(processor.groupId() + processor.artifactId(), processor);
    }

    public static void process(MavenProject project, Plugin plugin) {
        PluginProcessor processor = processors.get(plugin.getGroupId() + plugin.getArtifactId());
        if (processor != null) {
            processor.process(project, plugin);
        }
    }
}
