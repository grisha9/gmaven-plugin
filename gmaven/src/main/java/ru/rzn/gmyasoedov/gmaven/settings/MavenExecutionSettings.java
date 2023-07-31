package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

import static ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.OutputLevelType.DEFAULT;

public class MavenExecutionSettings extends ExternalSystemExecutionSettings {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private final MavenExecutionWorkspace executionWorkspace = new MavenExecutionWorkspace();
    @NotNull
    private final DistributionSettings distributionSettings;
    @Nullable
    private String javaHome;
    @Nullable
    private String jdkName;
    @Nullable
    private String myIdeProjectPath;
    @Nullable
    private String threadCount;
    private final boolean offlineWork;
    private boolean resolveModulePerSourceSet = false;
    private boolean useQualifiedModuleNames = false;
    private boolean nonRecursive = false;
    private boolean updateSnapshots = false;
    @NotNull
    private ProjectSettingsControlBuilder.OutputLevelType outputLevel = DEFAULT;

    public MavenExecutionSettings(@NotNull DistributionSettings distributionSettings,
                                  @Nullable String vmOptions,
                                  boolean nonRecursive,
                                  boolean offlineWork) {
        this.distributionSettings = Objects.requireNonNull(distributionSettings);
        if (vmOptions != null) {
            withVmOptions(ParametersListUtil.parse(vmOptions, true, true));
        }
        this.nonRecursive = nonRecursive;
        this.offlineWork = offlineWork;
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

    public void setJdkName(@Nullable String jdkName) {
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

    @Nullable
    public String getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(@Nullable String threadCount) {
        this.threadCount = threadCount;
    }

    public boolean isNonRecursive() {
        return nonRecursive;
    }

    public void setNonRecursive(boolean nonRecursive) {
        this.nonRecursive = nonRecursive;
    }

    public boolean isUpdateSnapshots() {
        return updateSnapshots;
    }

    public void setUpdateSnapshots(boolean updateSnapshots) {
        this.updateSnapshots = updateSnapshots;
    }

    @NotNull
    public ProjectSettingsControlBuilder.OutputLevelType getOutputLevel() {
        return outputLevel;
    }

    public void setOutputLevel(@NotNull ProjectSettingsControlBuilder.OutputLevelType outputLevel) {
        this.outputLevel = outputLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MavenExecutionSettings that = (MavenExecutionSettings) o;

        if (offlineWork != that.offlineWork) return false;
        if (resolveModulePerSourceSet != that.resolveModulePerSourceSet) return false;
        if (useQualifiedModuleNames != that.useQualifiedModuleNames) return false;
        if (!executionWorkspace.equals(that.executionWorkspace)) return false;
        if (!distributionSettings.equals(that.distributionSettings)) return false;
        if (!Objects.equals(javaHome, that.javaHome)) return false;
        if (!Objects.equals(jdkName, that.jdkName)) return false;
        return Objects.equals(myIdeProjectPath, that.myIdeProjectPath);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + executionWorkspace.hashCode();
        result = 31 * result + distributionSettings.hashCode();
        result = 31 * result + (offlineWork ? 1 : 0);
        result = 31 * result + (javaHome != null ? javaHome.hashCode() : 0);
        result = 31 * result + (jdkName != null ? jdkName.hashCode() : 0);
        result = 31 * result + (myIdeProjectPath != null ? myIdeProjectPath.hashCode() : 0);
        result = 31 * result + (resolveModulePerSourceSet ? 1 : 0);
        result = 31 * result + (useQualifiedModuleNames ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return distributionSettings.toString();
    }
}
