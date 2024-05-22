package ru.rzn.gmyasoedov.model.reader.plugins;


import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import ru.rzn.gmyasoedov.model.reader.utils.GUtils;
import ru.rzn.gmyasoedov.model.reader.utils.MavenContextUtils;
import ru.rzn.gmyasoedov.model.reader.utils.MavenJDOMUtil;
import ru.rzn.gmyasoedov.model.reader.utils.PluginUtils;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static ru.rzn.gmyasoedov.model.reader.utils.MavenContextUtils.EXCLUDED_PATHS;

public abstract class AbstractGroovyPluginProcessor {

//    <configuration>
//  <sources>
//    <source>
//    <directory>${project.basedir}/src/main/java</directory>
//      <includes>
//        <include>**/*.groovy</include>
//      </includes>
//    </source>
//  </sources>
//  <testSources>
//    <testSource>
//    <directory>${project.basedir}/src/test/java</directory>
//      <includes>
//        <include>**/*.groovy</include>
//      </includes>
//    </testSource>
//  </testSources>
//<configuration>

    public static void process(MavenProject project, Plugin plugin) {
        Object stubConfiguration = null;
        Object testStubConfiguration = null;
        List<PluginExecution> executions = plugin.getExecutions();
        for (PluginExecution execution : executions) {
            List<String> goals = execution.getGoals();
            if (goals == null) continue;
            if (goals.contains("compile")) {
                List<String> paths = getPathList(execution.getConfiguration(), project, false);
                for (String path : paths) {
                    project.addCompileSourceRoot(path);
                }
            }

            if (goals.contains("testCompile") || goals.contains("compileTests")) {
                List<String> paths = getPathList(execution.getConfiguration(), project, true);
                for (String path : paths) {
                    project.addCompileSourceRoot(path);
                }
            }

            if (goals.contains("generateStubs")) {
                stubConfiguration = execution.getConfiguration();
            }
            if (goals.contains("generateTestStubs")) {
                testStubConfiguration = execution.getConfiguration();
            }
            MavenContextUtils.addListStringValue(project, EXCLUDED_PATHS, getExcludedPath(project, stubConfiguration));
            MavenContextUtils.addListStringValue(project, EXCLUDED_PATHS, getExcludedPath(project, testStubConfiguration));
        }
    }

    private static String getExcludedPath(MavenProject mavenProject, Object config) {
        if (config == null) return getDefaultExcludedDir(mavenProject);
        String outputDirectory = PluginUtils.getValue(config, "outputDirectory");
        return outputDirectory != null ? GUtils.getAbsolutePath(outputDirectory, mavenProject)
                : getDefaultExcludedDir(mavenProject);
    }

    private static String getDefaultExcludedDir(MavenProject mavenProject) {
        return Paths.get(mavenProject.getBuild().getDirectory(), "generated-sources", "groovy-stubs").toString();
    }

    private static List<String> getPathList(Object config, MavenProject mavenProject, Boolean isTest) {
        Xpp3Dom currentTag = config instanceof Xpp3Dom ? ((Xpp3Dom) config) : null;

        if (currentTag == null) {
            return getDefaultPath(mavenProject, isTest);
        }
        List<String> dirs = MavenJDOMUtil.findChildrenValuesByPath(currentTag, "sources", "fileset.directory");
        if (dirs.isEmpty()) {
            return getDefaultPath(mavenProject, isTest);
        }
        return dirs;

        /*if (currentTag == null) return Collections.emptyList();
        currentTag = currentTag.getChild(sourcesName);
        if (currentTag == null) return Collections.emptyList();
        Xpp3Dom[] sources = currentTag.getChildren(sourceName);
        if (sources == null) return Collections.emptyList();
        ArrayList<String> result = new ArrayList<>(1);
        for (Xpp3Dom source : sources) {
            Xpp3Dom directory = source.getChild("directory");
            if (directory != null && directory.getValue() != null) {
                result.add(directory.getValue());
            }
        }
        return result;*/
    }

    private static List<String> getDefaultPath(MavenProject mavenProject, Boolean isTest) {
        String sourceFolderName = isTest ? "test" : "main";
        return Collections.singletonList(
                Paths.get(mavenProject.getBasedir().getAbsolutePath(), "src", sourceFolderName, "groovy").toString()
        );
    }
}
