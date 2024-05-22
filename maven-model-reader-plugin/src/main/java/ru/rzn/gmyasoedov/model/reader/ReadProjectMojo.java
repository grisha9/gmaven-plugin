package ru.rzn.gmyasoedov.model.reader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import ru.rzn.gmyasoedov.model.reader.plugins.PluginProcessorManager;

import java.util.*;

import static org.apache.maven.plugins.annotations.LifecyclePhase.NONE;

@Mojo(name = "read", defaultPhase = NONE, aggregator = true, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class ReadProjectMojo extends AbstractMojo {
    private static final String GMAVEN_PLUGINS = "gmaven.plugins";


    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;


    @Override
    public void execute() throws MojoExecutionException {
        Set<String> gPluginSet = getPluginForBodyProcessing();
        getLog().info("ResolveProjectMojo: " + gPluginSet);
        if (session.getAllProjects() == null) return;
        for (MavenProject mavenProject : session.getAllProjects()) {
            resolvePluginBody(mavenProject, gPluginSet);
        }
    }

    public void resolvePluginBody(MavenProject project, Set<String> gPlugins) throws MojoExecutionException {
        Model mavenModel = project.getModel();
        if (gPlugins.isEmpty() || mavenModel == null) return;

        Build build = mavenModel.getBuild();
        if (build != null) {
            List<Plugin> plugins = build.getPlugins();
            if (plugins != null) {
                for (Plugin each : plugins) {
                    processPlugin(each, gPlugins, project);
                }
            }
        }
    }

    private void processPlugin(Plugin each, Set<String> gPlugins, MavenProject project) {
        String pluginKey = each.getGroupId() + ":" + each.getArtifactId();
        if (!gPlugins.contains(pluginKey)) return;
        PluginProcessorManager.process(project, each);
        Map<String, Object> pluginBody = convertPluginBody(each);
        if (!pluginBody.isEmpty()) {
            String key = "gPlugin" + pluginKey;
            project.setContextValue(key, pluginBody);
        }
    }

    private Map<String, Object> convertPluginBody(Plugin plugin) {
        List<Map<String, Object>> executions = new ArrayList<>(plugin.getExecutions().size());
        for (PluginExecution each : plugin.getExecutions()) {
            executions.add(convertExecution(each));
        }
        Map<String, Object> result = new HashMap<>(5);
        result.put("executions", executions);
        result.put("configuration", convertConfiguration(plugin.getConfiguration()));
        return result;
    }

    private static Map<String, Object> convertExecution(PluginExecution execution) {
        Map<String, Object> result = new HashMap<>(5);
        result.put("id", execution.getId());
        result.put("phase", execution.getPhase());
        result.put("goals", execution.getGoals());
        result.put("configuration", convertConfiguration(execution.getConfiguration()));
        return result;
    }

    private static String convertConfiguration(Object config) {
        if (config instanceof Xpp3Dom) {
            return config.toString();
        } else {
            return null;
        }
    }

    private Set<String> getPluginForBodyProcessing() {
        String gPlugins = System.getProperty(GMAVEN_PLUGINS, "");
        if (gPlugins.isEmpty()) return Collections.emptySet();
        String[] gPluginsArray = gPlugins.split(";");
        HashSet<String> gPluginSet = new HashSet<>(gPluginsArray.length * 2);
        Collections.addAll(gPluginSet, gPluginsArray);
        return gPluginSet;
    }
}