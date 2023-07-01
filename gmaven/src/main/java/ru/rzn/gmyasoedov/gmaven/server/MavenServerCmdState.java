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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.PathUtil;
import com.intellij.util.net.NetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenCompilerFullImportPlugin;
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static ru.rzn.gmyasoedov.serverapi.GMavenServer.*;

public class MavenServerCmdState extends CommandLineState {
    private static final Logger LOG = Logger.getInstance(MavenServerCmdState.class);

    private final Sdk jdk;
    private final Path mavenPath;
    private final Path workingDirectory;
    private final String vmOptions;
    private final Integer debugPort;
    private final boolean skipTests;

    public MavenServerCmdState(@NotNull Sdk jdk, @NotNull Path mavenPath,
                               @Nullable String vmOptions,
                               @NotNull Path workingDirectory,
                               boolean skipTests) {
        super(null);
        this.jdk = jdk;
        this.mavenPath = mavenPath;
        this.workingDirectory = workingDirectory;
        this.vmOptions = vmOptions;
        this.skipTests = skipTests;
        this.debugPort = getDebugPort();
    }

    protected SimpleJavaParameters createJavaParameters() {
        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(jdk);
        params.setWorkingDirectory(workingDirectory.toFile());
        setupMavenOpts(params);
        setupDebugParam(params);
        setupClasspath(params);
        setupGmavenPluginsProperty(params);
        processVmOptions(vmOptions, params);
        params.setMainClass("ru.rzn.gmyasoedov.gmaven.server.RemoteGMavenServer");
        return params;
    }

    private void setupGmavenPluginsProperty(SimpleJavaParameters params) {
        List<MavenFullImportPlugin> extensionList = MavenFullImportPlugin.EP_NAME.getExtensionList();
        List<String> pluginsForImport = new ArrayList<>(extensionList.size());
        for (MavenFullImportPlugin plugin : extensionList) {
            pluginsForImport.add(plugin.getKey());
            String annotationPath = plugin instanceof MavenCompilerFullImportPlugin
                    ? ((MavenCompilerFullImportPlugin) plugin).getAnnotationProcessorPath() : null;
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
        if (skipTests) {
            params.getVMParametersList().addProperty("skipTests", "true");
        }
    }

    private void processVmOptions(String myVmOptions, SimpleJavaParameters params) {
        @Nullable String xmxProperty = null;
        @Nullable String xmsProperty = null;

        if (myVmOptions != null) {
            ParametersList mavenOptsList = new ParametersList();
            mavenOptsList.addParametersString(myVmOptions);

            for (String param : mavenOptsList.getParameters()) {
                if (param.startsWith("-Xmx")) {
                    xmxProperty = param;
                    continue;
                }
                if (param.startsWith("-Xms")) {
                    xmsProperty = param;
                    continue;
                }
                if (Registry.is("gmaven.vm.remove.javaagent") && param.startsWith("-javaagent")) {
                    continue;
                }
                params.getVMParametersList().add(param);
            }
        }

        String embedderXmx = System.getProperty("idea.maven.embedder.xmx");
        if (embedderXmx != null) {
            xmxProperty = "-Xmx" + embedderXmx;
        } else if (xmxProperty == null) {
            xmxProperty = getMaxXmxStringValue("-Xmx768m", xmsProperty);
        }
        params.getVMParametersList().add(xmsProperty);
        params.getVMParametersList().add(xmxProperty);
    }

    private void setupDebugParam(SimpleJavaParameters params) {
        if (debugPort != null) {
            params.getVMParametersList().addProperty(SERVER_DEBUG_PROPERTY, Boolean.TRUE.toString());
            params.getVMParametersList().addParametersString(
                    "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:" + debugPort);
        }
    }

    private void setupMainExt(SimpleJavaParameters params) {
        //it is critical to setup maven.ext.class.path for maven >=3.6, otherwise project extensions will not be loaded
        //  MavenUtil.addEventListener(myDistribution.getVersion(), params);
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
    public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
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

    @Nullable
    static String getMaxXmxStringValue(@Nullable String memoryValueA, @Nullable String memoryValueB) {
        MemoryProperty propertyA = MemoryProperty.valueOf(memoryValueA);
        MemoryProperty propertyB = MemoryProperty.valueOf(memoryValueB);
        if (propertyA != null && propertyB != null) {
            MemoryProperty maxMemoryProperty = propertyA.valueBytes > propertyB.valueBytes ? propertyA : propertyB;
            return MemoryProperty.of(MemoryProperty.MemoryPropertyType.XMX, maxMemoryProperty.valueBytes)
                    .toString(maxMemoryProperty.unit);
        }
        return Optional
                .ofNullable(propertyA).or(() -> Optional.ofNullable(propertyB))
                .map(property -> MemoryProperty.of(MemoryProperty.MemoryPropertyType.XMX, property.valueBytes)
                        .toString(property.unit))
                .orElse(null);
    }

    private static class MemoryProperty {
        private static final Pattern MEMORY_PROPERTY_PATTERN = Pattern.compile("^(-Xmx|-Xms)(\\d+)([kK]|[mM]|[gG])?$");
        final String type;
        final long valueBytes;
        final MemoryUnit unit;

        private MemoryProperty(@NotNull String type, long value, @Nullable String unit) {
            this.type = type;
            this.unit = unit != null ? MemoryUnit.valueOf(unit.toUpperCase()) : MemoryUnit.B;
            this.valueBytes = value * this.unit.ratio;
        }

        @NotNull
        public static MemoryProperty of(@NotNull MemoryPropertyType propertyType, long bytes) {
            return new MemoryProperty(propertyType.type, bytes, MemoryUnit.B.name());
        }

        @Nullable
        public static MemoryProperty valueOf(@Nullable String value) {
            if (value == null) return null;
            Matcher matcher = MEMORY_PROPERTY_PATTERN.matcher(value);
            if (matcher.find()) {
                return new MemoryProperty(matcher.group(1), Long.parseLong(matcher.group(2)), matcher.group(3));
            }
            LOG.warn(value + " not match " + MEMORY_PROPERTY_PATTERN);
            return null;
        }

        @Override
        public String toString() {
            return toString(unit);
        }

        public String toString(MemoryUnit unit) {
            return type + valueBytes / unit.ratio + unit.name().toLowerCase();
        }

        private enum MemoryUnit {
            B(1), K(B.ratio * 1024), M(K.ratio * 1024), G(M.ratio * 1024);
            final int ratio;

            MemoryUnit(int ratio) {
                this.ratio = ratio;
            }
        }

        private enum MemoryPropertyType {
            XMX("-Xmx"), XMS("-Xms");
            private final String type;

            MemoryPropertyType(String type) {
                this.type = type;
            }
        }
    }
}
