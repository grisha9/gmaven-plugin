package ru.rzn.gmyasoedov.model.reader.plugins;


import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import ru.rzn.gmyasoedov.model.reader.utils.GUtils;
import ru.rzn.gmyasoedov.model.reader.utils.PluginUtils;

import java.util.List;

public class BuildHelperMavenPluginProcessor implements PluginProcessor {

    @Override
    public String groupId() {
        return "org.codehaus.mojo";
    }

    @Override
    public String artifactId() {
        return "build-helper-maven-plugin";
    }

    @Override
    public void process(MavenProject project, Plugin plugin) {
        List<PluginExecution> executions = plugin.getExecutions();
        for (PluginExecution execution : executions) {
            List<String> goals = execution.getGoals();
            if (goals == null) continue;
            if (goals.contains("add-source")) {
                List<String> pathList = PluginUtils.getPathList(execution.getConfiguration(), "sources");
                for (String path : pathList) {
                    project.addCompileSourceRoot(path);
                }
            }

            if (goals.contains("add-test-source")) {
                List<String> pathList = PluginUtils.getPathList(execution.getConfiguration(), "sources");
                for (String path : pathList) {
                    project.addTestCompileSourceRoot(path);
                }
            }

            if (goals.contains("add-resource")) {
                List<String> pathList = PluginUtils.getPathList(execution.getConfiguration(), "resources");
                for (String path : pathList) {
                    String absolutePath = GUtils.getAbsolutePath(path, project);
                    if (absolutePath != null) {
                        Resource resource = new Resource();
                        resource.setDirectory(absolutePath);
                        project.addResource(resource);
                    }
                }
            }
            if (goals.contains("add-test-resource")) {
                List<String> pathList = PluginUtils.getPathList(execution.getConfiguration(), "resources");
                for (String path : pathList) {
                    String absolutePath = GUtils.getAbsolutePath(path, project);
                    if (absolutePath != null) {
                        Resource resource = new Resource();
                        resource.setDirectory(absolutePath);
                        project.addTestResource(resource);
                    }
                }
            }
        }
    }
}
