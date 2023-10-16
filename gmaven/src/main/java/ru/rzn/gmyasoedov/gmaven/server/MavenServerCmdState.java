package ru.rzn.gmyasoedov.gmaven.server;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.PathUtil;
import com.intellij.util.net.NetUtils;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin;
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static ru.rzn.gmyasoedov.serverapi.GMavenServer.*;

public class MavenServerCmdState extends CommandLineState {
    private static final String DEFAULT_XMX = "-Xmx768m";

    private final @NotNull GServerRequest request;
    private final Path mavenPath;
    private final Path workingDirectory;
    private final List<String> jvmConfigOptions;

    private final Integer debugPort;

    public MavenServerCmdState(@NotNull GServerRequest request,
                               @NotNull Path workingDirectory,
                               @NotNull List<String> jvmConfigOptions) {
        super(null);
        this.request = request;
        this.mavenPath = request.getMavenPath();
        this.workingDirectory = workingDirectory;
        this.jvmConfigOptions = jvmConfigOptions;
        this.debugPort = getDebugPort();
    }

    protected SimpleJavaParameters createJavaParameters() {
        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(request.getSdk());
        params.setWorkingDirectory(workingDirectory.toFile());
        setupMavenOpts(params);
        setupDebugParam(params);
        setupClasspath(params);
        setupGmavenPluginsProperty(params);
        processVmOptions(jvmConfigOptions, params);
        params.setMainClass("ru.rzn.gmyasoedov.gmaven.server.RemoteGMavenServer");
        return params;
    }

    private void setupGmavenPluginsProperty(SimpleJavaParameters params) {
        List<MavenFullImportPlugin> extensionList = MavenFullImportPlugin.EP_NAME.getExtensionList();
        List<String> pluginsForImport = new ArrayList<>(extensionList.size());
        for (MavenFullImportPlugin plugin : extensionList) {
            pluginsForImport.add(plugin.getKey());
            String annotationPath = plugin instanceof MavenCompilerFullImportPlugin
                    ? ((MavenCompilerFullImportPlugin) plugin).getAnnotationProcessorTagName() : null;
            if (StringUtilRt.isEmpty(annotationPath)) continue;

            params.getVMParametersList()
                    .addProperty(format(GMAVEN_PLUGIN_ANNOTATION_PROCESSOR, plugin.getArtifactId()), annotationPath);
        }

        if (!pluginsForImport.isEmpty()) {
            params.getVMParametersList().addProperty(GMAVEN_PLUGINS, String.join(";", pluginsForImport));
        }
    }

    private void setupClasspath(SimpleJavaParameters params) {
        String mavenServerJarPathString;
        String mavenExtClassesJarPathString;
        try {
            mavenServerJarPathString = PathUtil
                    .getJarPathForClass(Class.forName("ru.rzn.gmyasoedov.gmaven.server.RemoteGMavenServer"));
            mavenExtClassesJarPathString = PathUtil
                    .getJarPathForClass(Class.forName("ru.rzn.gmyasoedov.event.handler.EventSpyResultHolder"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        String mavenServerApiJarPathString = PathUtil.getJarPathForClass(GMavenServer.class);
        params.getClassPath().add(mavenServerJarPathString);
        params.getClassPath().add(mavenServerApiJarPathString);
        params.getClassPath().add(getPlexusClassWorlds(mavenPath));
        params.getClassPath().addAll(collectIdeaRTLibraries());

        params.getVMParametersList().addProperty(MAVEN_EXT_CLASS_PATH_PROPERTY, mavenExtClassesJarPathString);
        params.getVMParametersList().addProperty(GMAVEN_HOME, mavenPath.toAbsolutePath().toString());
        request.getSettings().getEnv().forEach((k, v) -> addProperty(params, k, v));
    }

    private static void addProperty(SimpleJavaParameters params, String k, String v) {
        if (v != null) {
            params.getVMParametersList().addProperty(k, v);
        } else {
            params.getVMParametersList().addProperty(k);
        }
    }

    private void processVmOptions(List<String> jvmConfigOptions, SimpleJavaParameters params) {
        boolean hasXmxProperty = false;
        boolean hasXmsProperty = false;
        List<String> vmOptions = new ArrayList<>(jvmConfigOptions);
        vmOptions.addAll(request.getSettings().getJvmArguments());
        for (String param : vmOptions) {
            if (param.startsWith("-Xmx")) {
                hasXmxProperty = true;
            }
            if (param.startsWith("-Xms")) {
                hasXmsProperty = true;
            }
            if (Registry.is("gmaven.vm.remove.javaagent") && param.startsWith("-javaagent")) {
                continue;
            }
            params.getVMParametersList().add(param);
        }
        if (!hasXmxProperty && !hasXmsProperty
                && (request.getSettings().isNonRecursive()) || request.getInstallGMavenPlugin()) {
            params.getVMParametersList().add(DEFAULT_XMX);
        }
    }

    private void setupDebugParam(SimpleJavaParameters params) {
        if (debugPort != null) {
            params.getVMParametersList().addProperty(SERVER_DEBUG_PROPERTY, Boolean.TRUE.toString());
            params.getVMParametersList().addParametersString(
                    "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:" + debugPort);
        }
    }

    private static void configureSslRelatedOptions(Map<String, String> defs) {
        for (Map.Entry<Object, Object> each : System.getProperties().entrySet()) {
            Object key = each.getKey();
            Object value = each.getValue();
            if (key instanceof String && value instanceof String && ((String) key).startsWith("javax.net.ssl")) {
                defs.put((String) key, (String) value);
            }
        }
    }

    public static void setupMavenOpts(@NotNull SimpleJavaParameters params) {
        String mavenOpts = System.getenv("MAVEN_OPTS");
        Map<String, String> mavenOptsMap;
        if (StringUtil.isNotEmpty(mavenOpts)) {
            ParametersList mavenOptsList = new ParametersList();
            mavenOptsList.addParametersString(mavenOpts);
            mavenOptsMap = mavenOptsList.getProperties();
        } else {
            mavenOptsMap = new HashMap<>();
        }
        configureSslRelatedOptions(mavenOptsMap);
        mavenOptsMap.put("java.awt.headless", "true");

        for (Map.Entry<String, String> each : mavenOptsMap.entrySet()) {
            params.getVMParametersList().defineProperty(each.getKey(), each.getValue());
        }
        params.getVMParametersList().defineProperty("GMAVEN", getIdeaVersionToPassToMavenProcess());
    }


    private Integer getDebugPort() {
        if (Registry.is("gmaven.server.debug")) {
            try {
                return NetUtils.findAvailableSocketPort();
            } catch (IOException e) {
                MavenLog.LOG.warn(e);
            }
        }
        return null;
    }

    protected @NotNull List<String> collectIdeaRTLibraries() {
        return new ArrayList<>(Set.of(
                PathUtil.getJarPathForClass(StringUtilRt.class),//util-rt
                PathUtil.getJarPathForClass(NotNull.class)));//annotations-java5
    }

    private static @NotNull String getPlexusClassWorlds(@NotNull Path mavenPath) {
        Path mavenBootPath = mavenPath.resolve("boot");
        String bootJarPrefix = Registry.stringValue("gmaven.boot.jar.prefix");
        try {
            Optional<String> bootJarPath;
            try (Stream<Path> files = Files.walk(mavenBootPath, 1)) {
                bootJarPath = files.filter(f -> isBootJar(f, bootJarPrefix))
                        .map(Path::toString)
                        .findFirst();
            }
            if (bootJarPath.isEmpty()) {
                try (Stream<Path> files = Files.walk(mavenPath)) {
                    bootJarPath = files.filter(f -> isBootJar(f, bootJarPrefix))
                            .map(Path::toString)
                            .findFirst();
                }
            }
            return bootJarPath.orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException("maven boot jar not found", e);
        }
    }

    private static boolean isBootJar(Path f, String bootJarPrefix) {
        String name = f.getFileName().toString();
        return name.startsWith(bootJarPrefix) && name.endsWith(".jar");
    }

    @NotNull
    @Override
    public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner)
            throws ExecutionException {
        ProcessHandler processHandler = startProcess();
        return new DefaultExecutionResult(processHandler);
    }

    @Override
    @NotNull
    protected ProcessHandler startProcess() throws ExecutionException {
        SimpleJavaParameters params = createJavaParameters();
        GeneralCommandLine commandLine = params.toCommandLine();
        OSProcessHandler processHandler = new OSProcessHandler.Silent(commandLine);
        processHandler.setShouldDestroyProcessRecursively(false);
        return processHandler;
    }

    public static String getIdeaVersionToPassToMavenProcess() {
        return ApplicationInfoImpl.getShadowInstance().getMajorVersion() + "."
                + ApplicationInfoImpl.getShadowInstance().getMinorVersion();
    }
}
