package ru.rzn.gmyasoedov.gmaven.server;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.rmi.RemoteProcessSupport;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionWorkspace;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties.getJvmConfig;

public class GServerRemoteProcessSupport extends RemoteProcessSupport<Object, GMavenServer, Object> {
    private final @NotNull GServerRequest request;
    private final List<String> jvmConfigOptions;
    private final Path workingDirectory;
    private final boolean isImport;

    public GServerRemoteProcessSupport(@NotNull GServerRequest request) {
        this(request, true);
    }

    public GServerRemoteProcessSupport(@NotNull GServerRequest request, boolean isImport) {
        super(GMavenServer.class);
        this.isImport = isImport;
        this.request = request;
        this.workingDirectory = getWorkingDirectory(request);
        this.jvmConfigOptions = getJvmConfigOptions(this.workingDirectory);
    }

    public static List<String> getJvmConfigOptions(Path workingDirectory) {
        String jvmConfig = getJvmConfig(workingDirectory);
        return StringUtil.isEmpty(jvmConfig)
                ? Collections.emptyList() : ParametersListUtil.parse(jvmConfig, true, true);
    }

    @Override
    protected void fireModificationCountChanged() {
    }

    @Override
    protected String getName(@NotNull Object file) {
        return "GServerRemoteProcessSupport";
    }

    public ExternalSystemTaskId getId() {
        return request.getTaskId();
    }

    @Override
    protected void logText(@NotNull Object configuration,
                           @NotNull ProcessEvent event,
                           @NotNull Key outputType) {
        String text = StringUtil.notNullize(event.getText());
        if (Registry.is("gmaven.server.debug")) {
            System.out.println(text);
        }
        if (request.getListener() != null) {
            request.getListener().onTaskOutput(request.getTaskId(), text, true);
        }
    }

    @Override
    protected RunProfileState getRunProfileState(@NotNull Object o,
                                                 @NotNull Object configuration,
                                                 @NotNull Executor executor) {
        return new MavenServerCmdState(request, workingDirectory, jvmConfigOptions, isImport);
    }

    private static Path getWorkingDirectory(@NotNull GServerRequest request) {
        MavenExecutionWorkspace workspace = request.getSettings().getExecutionWorkspace();
        if (workspace.getMultiModuleProjectDirectory() != null) {
            return Path.of(workspace.getMultiModuleProjectDirectory());
        } else {
            return request.getProjectPath().toFile().isDirectory()
                    ? request.getProjectPath() : request.getProjectPath().getParent();
        }
    }
}