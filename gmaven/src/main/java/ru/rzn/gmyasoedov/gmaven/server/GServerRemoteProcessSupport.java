package ru.rzn.gmyasoedov.gmaven.server;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.rmi.RemoteProcessSupport;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties.getJvmConfig;

public class GServerRemoteProcessSupport extends RemoteProcessSupport<Object, GMavenServer, Object> {
    private final ExternalSystemTaskId id;
    private final Sdk jdk;
    private final List<String> jvmConfigOptions;
    private final Path mavenPath;
    private final Path workingDirectory;
    private final ExternalSystemTaskNotificationListener systemTaskNotificationListener;
    private final MavenExecutionSettings executionSettings;

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
    }


    @Override
    protected void fireModificationCountChanged() {
    }

    @Override
    protected String getName(@NotNull Object file) {
        return "GServerRemoteProcessSupport";
    }

    public ExternalSystemTaskId getId() {
        return id;
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    @Override
    protected void sendDataAfterStart(ProcessHandler handler) {
        if (handler.getProcessInput() == null) {
            return;
        }
        OutputStreamWriter writer = new OutputStreamWriter(handler.getProcessInput(), StandardCharsets.UTF_8);
        try {
            writer.write("token=" + UUID.randomUUID());
            writer.write(System.lineSeparator());
            writer.flush();
            MavenLog.LOG.info("Sent token to maven server");
        } catch (IOException e) {
            MavenLog.LOG.warn("Cannot send token to maven server", e);
        }
    }

    @Override
    protected void logText(@NotNull Object configuration,
                           @NotNull ProcessEvent event,
                           @NotNull Key outputType) {
        String text = StringUtil.notNullize(event.getText());
        if (systemTaskNotificationListener != null) {
            systemTaskNotificationListener.onTaskOutput(id, text, true);
        }

    }

    @Override
    protected RunProfileState getRunProfileState(@NotNull Object o,
                                                 @NotNull Object configuration,
                                                 @NotNull Executor executor) {
        return new MavenServerCmdState(jdk, mavenPath, workingDirectory, jvmConfigOptions, executionSettings);
    }
}