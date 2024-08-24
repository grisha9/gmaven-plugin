package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.TasksNode;

import java.util.Collection;

import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.BUILTIN_TASKS_DATA_NODE_ORDER;

@Order(BUILTIN_TASKS_DATA_NODE_ORDER + 1)
public class PluginNodes extends TasksNode {

    @SuppressWarnings("unchecked")
    public PluginNodes(ExternalProjectsView externalProjectsView, final Collection<? extends DataNode<?>> dataNodes) {
        super(externalProjectsView, dataNodes);
    }

    @Override
    public String getName() {
        return "Plugins";
    }
}
