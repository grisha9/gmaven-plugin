package ru.rzn.gmyasoedov.gmaven.server;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.rmi.RemoteProcessSupport;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.server.wsl.WslMavenCmdState;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.intellij.openapi.util.SystemInfo.isWindows;
import static ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties.getJvmConfig;

public class GServerRemoteProcessSupport extends RemoteProcessSupport<Object, GMavenServer, Object> {
    private final ExternalSystemTaskId id;
    private final Sdk jdk;
    private final List<String> jvmConfigOptions;
    private final Path mavenPath;
    private final Path workingDirectory;
    private final ExternalSystemTaskNotificationListener systemTaskNotificationListener;
    private final MavenExecutionSettings executionSettings;
    private final WSLDistribution wslDistribution;

    public GServerRemoteProcessSupport(@NotNull GServerRequest request) {
        super(GMavenServer.class);
        this.id = request.getTaskId();
        this.jdk = request.getSdk();
        this.mavenPath = request.getMavenPath();
        this.workingDirectory = request.getProjectPath().toFile().isDirectory()
                ? request.getProjectPath() : request.getProjectPath().getParent();
        this.systemTaskNotificationListener = request.getListener();
        this.executionSettings = request.getSettings();
        String jvmConfig = getJvmConfig(workingDirectory);
        this.jvmConfigOptions = StringUtil.isEmpty(jvmConfig)
                ? Collections.emptyList() : ParametersListUtil.parse(jvmConfig, true, true);
        this.wslDistribution = isWindows ? WslPath.getDistributionByWindowsUncPath(workingDirectory.toString()) : null;
    }


    @Override
    protected void fireModificationCountChanged() {
    }

    @Override
    protected String getName(@NotNull Object file) {
        return getClass().getSimpleName();
    }

    @Override
    protected String getRemoteHost() {
        return wslDistribution == null ? super.getRemoteHost() : wslDistribution.getWslIpAddress().getHostAddress();
    }

    public ExternalSystemTaskId getId() {
        return id;
    }

    @Nullable
    public WSLDistribution getWslDistribution() {
        return wslDistribution;
    }

    @Override
    protected void logText(@NotNull Object configuration,
                           @NotNull ProcessEvent event,
                           @NotNull Key outputType) {
        String text = StringUtil.notNullize(event.getText());
        if (Registry.is("gmaven.server.debug")) {
            System.out.println(text);
        }
        if (systemTaskNotificationListener != null) {
            systemTaskNotificationListener.onTaskOutput(id, text, true);
        }
    }

    @Override
    protected RunProfileState getRunProfileState(@NotNull Object o,
                                                 @NotNull Object configuration,
                                                 @NotNull Executor executor) {
        return wslDistribution == null
                ? new MavenServerCmdState(jdk, mavenPath, workingDirectory, jvmConfigOptions, executionSettings)
                : getWslMavenCmdState(wslDistribution);
    }

    @NotNull
    private WslMavenCmdState getWslMavenCmdState(@NotNull WSLDistribution wslDist) {
        Path mavenWslPath = Optional.ofNullable(wslDist.getWslPath(mavenPath.toString()))
                .map(wslDist::getWindowsPath)
                .map(Path::of)
                .orElse(null);
        String jdkWslPath = Optional.ofNullable(jdk.getHomePath())
                .map(wslDist::getWslPath)
                .orElse(null);

        if (mavenWslPath == null || jdkWslPath == null) {
            throw new ExternalSystemException(
                    "Wsl paths incorrect. All paths should be correctly wsl path" + System.lineSeparator() +
                            "Project path: " + workingDirectory + System.lineSeparator() +
                            "Maven path: " + mavenWslPath + System.lineSeparator() +
                            "Jdk path: " + jdkWslPath
            );
        }
        return new WslMavenCmdState(
                this.wslDistribution, jdk, mavenWslPath, workingDirectory, jvmConfigOptions, executionSettings
        );
    }
}