package ru.rzn.gmyasoedov.gmaven.task;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings;

import java.util.List;

public class MavenTaskManager implements ExternalSystemTaskManager<MavenExecutionSettings> {

    @Override
    public boolean cancelTask(@NotNull ExternalSystemTaskId id,
                              @NotNull ExternalSystemTaskNotificationListener listener)
            throws ExternalSystemException {
        return false;
    }
}
