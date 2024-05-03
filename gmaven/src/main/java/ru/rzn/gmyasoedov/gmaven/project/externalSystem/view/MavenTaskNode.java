package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.TaskNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MavenTaskNode extends TaskNode {

    public MavenTaskNode(@NotNull ExternalProjectsView externalProjectsView, @NotNull DataNode<TaskData> dataNode) {
        super(externalProjectsView, dataNode);
    }

    @Override
    protected void sort(List<? extends ExternalSystemNode<?>> list) {
    }
}
