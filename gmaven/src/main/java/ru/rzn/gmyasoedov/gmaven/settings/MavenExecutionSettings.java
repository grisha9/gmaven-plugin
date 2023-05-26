// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Denis Zhdanov
 */
public class MavenExecutionSettings extends ExternalSystemExecutionSettings {

    private static final long serialVersionUID = 1L;

    @NotNull private final MavenExecutionWorkspace executionWorkspace = new MavenExecutionWorkspace();

    @NotNull
    private final DistributionSettings distributionSettings;

    @Nullable
    private final String serviceDirectory;
    private final boolean offlineWork;

    @Nullable
    private String javaHome;
    @Nullable
    private String jdkName;
    @Nullable
    private String myIdeProjectPath;
    private boolean resolveModulePerSourceSet = false;
    private boolean useQualifiedModuleNames = false;

    public MavenExecutionSettings(@NotNull DistributionSettings distributionSettings,
                                  @Nullable String serviceDirectory,
                                  boolean isOfflineWork) {
        this.distributionSettings = Objects.requireNonNull(distributionSettings);
        this.serviceDirectory = serviceDirectory;
        this.offlineWork = isOfflineWork;
    }

    public MavenExecutionSettings(@NotNull DistributionSettings distributionSettings,
                                  @Nullable String serviceDirectory,
                                  @Nullable String daemonVmOptions,
                                  boolean isOfflineWork) {
        this.distributionSettings = Objects.requireNonNull(distributionSettings);
        this.serviceDirectory = serviceDirectory;
        if (daemonVmOptions != null) {
            withVmOptions(ParametersListUtil.parse(daemonVmOptions));
        }
        offlineWork = isOfflineWork;
    }

    public void setIdeProjectPath(@Nullable String ideProjectPath) {
        myIdeProjectPath = ideProjectPath;
    }

    @Nullable
    public String getIdeProjectPath() {
        return myIdeProjectPath;
    }

    @NotNull
    public DistributionSettings getDistributionSettings() {
        return distributionSettings;
    }

    @Nullable
    public String getServiceDirectory() {
        return serviceDirectory;
    }

    @Nullable
    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(@Nullable String javaHome) {
        this.javaHome = javaHome;
    }

    @Nullable
    public String getJdkName() {
        return jdkName;
    }

    @Nullable
    public void setJdkName(String jdkName) {
        this.jdkName = jdkName;
    }

    public boolean isOfflineWork() {
        return offlineWork;
    }

    public boolean isResolveModulePerSourceSet() {
        return resolveModulePerSourceSet;
    }

    public void setResolveModulePerSourceSet(boolean resolveModulePerSourceSet) {
        this.resolveModulePerSourceSet = resolveModulePerSourceSet;
    }

    public boolean isUseQualifiedModuleNames() {
        return useQualifiedModuleNames;
    }

    public void setUseQualifiedModuleNames(boolean useQualifiedModuleNames) {
        this.useQualifiedModuleNames = useQualifiedModuleNames;
    }

    @NotNull
    public MavenExecutionWorkspace getExecutionWorkspace() {
        return executionWorkspace;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + distributionSettings.hashCode();
        result = 31 * result + (serviceDirectory != null ? serviceDirectory.hashCode() : 0);
        result = 31 * result + (jdkName != null ? jdkName.hashCode() : 0);
        result = 31 * result + (javaHome != null ? javaHome.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        MavenExecutionSettings that = (MavenExecutionSettings) o;
        if (!Objects.equals(distributionSettings, that.distributionSettings)) return false;
        if (!Objects.equals(jdkName, that.jdkName)) return false;
        if (!Objects.equals(javaHome, that.javaHome)) return false;
        if (!Objects.equals(serviceDirectory, that.serviceDirectory)) return false;
        return true;
    }

    @Override
    public String toString() {
        return distributionSettings.toString();
    }
}
