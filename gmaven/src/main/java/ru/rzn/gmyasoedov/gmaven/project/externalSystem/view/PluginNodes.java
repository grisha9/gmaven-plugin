// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.TaskNode;
import com.intellij.openapi.externalSystem.view.TasksNode;
import com.intellij.util.containers.MultiMap;

import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER)
public class PluginNodes extends TasksNode {

    private final MultiMap<String, TaskNode> myTasksMap = new MultiMap<>();

    @SuppressWarnings("unchecked")
    public PluginNodes(ExternalProjectsView externalProjectsView, final Collection<? extends DataNode<?>> dataNodes) {
        super(externalProjectsView, dataNodes);
    }

    @Override
    public String getName() {
        return "Plugins";
    }
}
