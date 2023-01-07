// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.server;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.rmi.RemoteProcessSupport;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;

public class GServerRemoteProcessSupport extends RemoteProcessSupport<Object, GMavenServer, Object> {
    protected final Sdk jdk;
    protected final String vmOptions;
    protected final Path mavenPath;

    @Nullable
    protected Consumer<ProcessEvent> onTerminate;

    public GServerRemoteProcessSupport(@NotNull Sdk jdk,
                                       @Nullable String vmOptions,
                                       @NotNull Path mavenPath) {
        super(GMavenServer.class);
        this.jdk = jdk;
        this.vmOptions = vmOptions;
        this.mavenPath = mavenPath;
    }


    @Override
    protected void fireModificationCountChanged() {
    }

    @Override
    protected String getName(@NotNull Object file) {
        return "GServerRemoteProcessSupport";
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

    public void onTerminate(Consumer<ProcessEvent> onTerminate) {
        this.onTerminate = onTerminate;
    }

    @Override
    protected void onProcessTerminated(ProcessEvent event) {
        Consumer<ProcessEvent> eventConsumer = onTerminate;

        if (eventConsumer != null) {
            eventConsumer.accept(event);
        }

        if (event.getExitCode() == 0) {
            return;
        }

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project p : openProjects) {
            ReadAction.run(() -> {
                if (p.isDisposed()) {
                    return;
                }
                //todo terminate import
            });
        }
    }

    @Override
    protected void logText(@NotNull Object configuration,
                           @NotNull ProcessEvent event,
                           @NotNull Key outputType) {
        String text = StringUtil.notNullize(event.getText());
        System.out.println(text);
    }

    @Override
    protected RunProfileState getRunProfileState(@NotNull Object o,
                                                 @NotNull Object configuration,
                                                 @NotNull Executor executor) {
        return new MavenServerCmdState(jdk, mavenPath, vmOptions);
    }
}