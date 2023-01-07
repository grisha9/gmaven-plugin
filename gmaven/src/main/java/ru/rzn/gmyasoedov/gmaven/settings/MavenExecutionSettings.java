// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Denis Zhdanov
 */
public class MavenExecutionSettings extends ExternalSystemExecutionSettings {

  private static final long serialVersionUID = 1L;

  /*@NotNull private final GradleExecutionWorkspace myExecutionWorkspace = new GradleExecutionWorkspace();*/

  @Nullable private final String mavenHome;

  @Nullable private final String serviceDirectory;
  private final boolean offlineWork;

  /*@NotNull private final DistributionType myDistributionType;*/
  @Nullable private String wrapperPropertyFile;

  @Nullable private String myJavaHome;
  @Nullable
  private String myIdeProjectPath;
  private boolean resolveModulePerSourceSet = true;
  private boolean useQualifiedModuleNames = false;

  public MavenExecutionSettings(@Nullable String mavenHome,
                                @Nullable String serviceDirectory,
                                /*@NotNull DistributionType distributionType,*/
                                boolean isOfflineWork) {
    this.mavenHome = mavenHome;
    this.serviceDirectory = serviceDirectory;
    /*myDistributionType = distributionType;*/
    this.offlineWork = isOfflineWork;
  }

  public MavenExecutionSettings(@Nullable String mavenHome,
                                @Nullable String serviceDirectory,
                               /* @NotNull DistributionType distributionType,*/
                                @Nullable String daemonVmOptions,
                                boolean isOfflineWork) {
    this.mavenHome = mavenHome;
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

  public String getMavenHome() {
    return mavenHome;
  }

  @Nullable
  public String getServiceDirectory() {
    return serviceDirectory;
  }

  @Nullable
  public String getJavaHome() {
    return myJavaHome;
  }

  public void setJavaHome(@Nullable String javaHome) {
    myJavaHome = javaHome;
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

  @Nullable
  public String getWrapperPropertyFile() {
    return wrapperPropertyFile;
  }

  public void setWrapperPropertyFile(@Nullable String wrapperPropertyFile) {
    this.wrapperPropertyFile = wrapperPropertyFile;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mavenHome != null ? mavenHome.hashCode() : 0);
    result = 31 * result + (serviceDirectory != null ? serviceDirectory.hashCode() : 0);
   /* result = 31 * result + myDistributionType.hashCode();*/
    result = 31 * result + (myJavaHome != null ? myJavaHome.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    MavenExecutionSettings that = (MavenExecutionSettings)o;
    /*if (myDistributionType != that.myDistributionType) return false;*/
    if (!Objects.equals(mavenHome, that.mavenHome)) return false;
    if (!Objects.equals(myJavaHome, that.myJavaHome)) return false;
    if (!Objects.equals(serviceDirectory, that.serviceDirectory)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "home: " + mavenHome + ", distributionType: " /*+ myDistributionType*/;
  }
}
