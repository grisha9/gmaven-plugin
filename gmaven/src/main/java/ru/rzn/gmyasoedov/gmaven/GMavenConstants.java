package ru.rzn.gmyasoedov.gmaven;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.util.registry.Registry.stringValue;
import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;
import static ru.rzn.gmyasoedov.serverapi.GMavenServer.MAVEN_MODEL_READER_PLUGIN_VERSION;

public final class GMavenConstants {
    public static final String BUNDLED_MAVEN_VERSION = "3.9.6";
    private static final String BUNDLED_DISTRIBUTION_URL =
            "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%s/apache-maven-%s-bin.zip";

    @NotNull
    @NlsSafe
    public static final String GMAVEN = "GMaven";
    public static final String POM_XML = "pom.xml";
    public static final String SUPER_POM_PREFIX = "pom-4.";
    public static final String PROFILES_XML = "profiles.xml";

    public static final String SCOPE_COMPILE = "compile";
    public static final String SCOPE_PROVIDED = "provided";
    public static final String SCOPE_RUNTIME = "runtime";
    public static final String SCOPE_TEST = "test";
    public static final String SCOPE_SYSTEM = "system";
    public static final String SCOPE_IMPORT = "import";
    public static final String M2 = ".m2";
    public static final String MODULE_PROP_BUILD_FILE = "buildFile";
    public static final String MODULE_PROP_PARENT_GA = "parentGA";
    public static final String MODULE_PROP_LOCAL_REPO = "localRepo";
    public static final String MODULE_PROP_HAS_DEPENDENCIES = "hasDependencies";
    public static final String APECTJ_COMPILER_LIB = "org.aspectj:aspectjtools";

    @NotNull
    @NonNls
    public static final String SOURCE_SET_MODULE_TYPE_KEY = "sourceSet";
    public static final String IDEA_PSI_EDIT_TOKEN = "IntellijIdeaRulezzz";
    public static final List<String> BASIC_PHASES =
            Arrays.asList("clean", "validate", "compile", "test", "package", "verify", "install", "deploy", "site");

    public static final String TASK_RESOLVE_PLUGINS = "dependency:resolve-plugins";
    public static final String TASK_DOWNLOAD_SOURCE = "dependency:sources";
    public static final String TASK_EFFECTIVE_POM = "help:effective-pom";
    public static final String TASK_DEPENDENCY_TREE = "ru.rzn.gmyasoedov:maven-model-reader-plugin:"
            + MAVEN_MODEL_READER_PLUGIN_VERSION + ":tree";
    public static final String DEPENDENCY_TREE_EVENT_SPY_CLASS = "ru.rzn.gmyasoedov.dependency.graph.DependencySpyConstants";

    @NotNull
    @NonNls
    public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(GMAVEN.toUpperCase(), GMAVEN);

    public static final int BUILTIN_TASKS_DATA_NODE_ORDER = 10;
    public static final int BUILTIN_DEPENDENCIES_DATA_NODE_ORDER = 20;

    private GMavenConstants() {
    }

    public static Set<String> getScopes() {
        return Set.of(SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_RUNTIME, SCOPE_TEST, SCOPE_SYSTEM);
    }

    public static String getBundledDistributionUrl() {
        String version = requireNonNullElse(stringValue("gmaven.bundled.wrapper.version"), BUNDLED_MAVEN_VERSION);
        return format(BUNDLED_DISTRIBUTION_URL, version, version);
    }
}
