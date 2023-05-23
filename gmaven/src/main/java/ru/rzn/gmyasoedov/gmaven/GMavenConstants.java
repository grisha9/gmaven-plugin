// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.intellij.openapi.util.registry.Registry.stringValue;
import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;

public final class GMavenConstants {
    private static final String BUNDLED_MAVEN_VERSION = "3.9.1";
    private static final String BUNDLED_DISTRIBUTION_URL =
            "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%s/apache-maven-%s-bin.zip";

    @NotNull
    @NlsSafe
    public static final String GMAVEN = "GMaven";
    public static final String POM_XML = "pom.xml";
    public static final String SUPER_POM_XML = "pom-4.0.0.xml";
    public static final String PROFILES_XML = "profiles.xml";

    public static final String SCOPE_COMPILE = "compile";
    public static final String SCOPE_PROVIDED = "provided";
    public static final String SCOPE_RUNTIME = "runtime";
    public static final String SCOPE_TEST = "test";
    public static final String SCOPE_SYSTEM = "system";
    public static final String SCOPE_IMPORT = "import";
    public static final String M2 = ".m2";
    public static final String TASK_LIFECYCLE = "Lifecycle";
    public static final String MODULE_PROP_BUILD_FILE = "buildFile";
    public static final List<String> BASIC_PHASES =
            List.of("clean", "validate", "compile", "test", "package", "verify", "install", "deploy", "site");

    @NotNull
    @NonNls
    public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(GMAVEN.toUpperCase(), GMAVEN);

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
