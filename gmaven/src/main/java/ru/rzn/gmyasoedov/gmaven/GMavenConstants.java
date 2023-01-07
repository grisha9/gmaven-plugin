// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.intellij.build.dependencies.BuildDependenciesCommunityRoot;
import org.jetbrains.intellij.build.impl.BundledMavenDownloader;

import java.nio.file.Path;
import java.util.Set;

public final class GMavenConstants {

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

    @NotNull
    @NonNls
    public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(GMAVEN.toUpperCase(), GMAVEN);

    public final static ClearableLazyValue<Path> embeddedMavenPath = ClearableLazyValue.create(() ->
            BundledMavenDownloader.downloadMavenDistribution(
            new BuildDependenciesCommunityRoot(Path.of(PathManager.getCommunityHomePath())))
    );

    private GMavenConstants() {
    }

    public static Set<String> getScopes() {
        return Set.of(SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_RUNTIME, SCOPE_TEST, SCOPE_SYSTEM);
    }
}
