package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.TaskNode;
import com.intellij.openapi.externalSystem.view.TasksNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER)
public class MavenTasksNode extends TasksNode {

    private final MultiMap<String, TaskNode> myTasksMap = MultiMap.createLinked();

    @SuppressWarnings("unchecked")
    public MavenTasksNode(ExternalProjectsView externalProjectsView, final Collection<? extends DataNode<?>> dataNodes) {
        super(externalProjectsView, null);

        if (dataNodes != null && !dataNodes.isEmpty()) {
            for (DataNode<?> dataNode : dataNodes) {
                if (!(dataNode.getData() instanceof TaskData)) continue;
                String group = ((TaskData)dataNode.getData()).getGroup();
                if (group == null) group = "other";
                MavenTaskNode taskNode = new MavenTaskNode(externalProjectsView, (DataNode<TaskData>) dataNode);
                myTasksMap.putValue(StringUtil.toLowerCase(group), taskNode);
            }
        }
    }

    @NotNull
    @Override
    protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
        final List<ExternalSystemNode<?>> result = new ArrayList<>();
        final boolean isGroup = getExternalProjectsView().getGroupTasks();
        if (isGroup) {
            for (Map.Entry<String, Collection<TaskNode>> collectionEntry : myTasksMap.entrySet()) {
                final String group = ObjectUtils.notNull(collectionEntry.getKey(), "other");
                final ExternalSystemNode<?> tasksGroupNode = new ExternalSystemNode<>(getExternalProjectsView(), null, null) {

                    @Override
                    protected void update(@NotNull PresentationData presentation) {
                        super.update(presentation);
                        presentation.setIcon(AllIcons.Nodes.ConfigFolder);
                    }

                    @Override
                    public String getName() {
                        return group;
                    }

                    @Override
                    public boolean isVisible() {
                        return super.isVisible() && hasChildren();
                    }


                    @Override
                    protected void sort(List<? extends ExternalSystemNode<?>> list) {
                    }
                };
                tasksGroupNode.addAll(collectionEntry.getValue());
                result.add(tasksGroupNode);
            }
        }
        else {
            result.addAll(myTasksMap.values());
        }
        return result;
    }

    @Override
    public String getName() {
        return "Lifecycle";
    }

    @Override
    protected void sort(List<? extends ExternalSystemNode<?>> list) {
    }
}
