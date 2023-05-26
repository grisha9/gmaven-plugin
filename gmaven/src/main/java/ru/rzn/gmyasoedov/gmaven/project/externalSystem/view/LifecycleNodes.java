package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.TaskNode;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.LifecycleData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID;

@Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER - 1)
public class LifecycleNodes extends ExternalSystemNode<Object> {

  private final List<TaskNode> tasks;

  @SuppressWarnings("unchecked")
  public LifecycleNodes(ExternalProjectsView externalProjectsView, final Collection<? extends DataNode<?>> dataNodes) {
    super(externalProjectsView, null, null);

    tasks = new ArrayList<>(dataNodes == null ? 0 : dataNodes.size());
    if (dataNodes != null && !dataNodes.isEmpty()) {
      for (DataNode<?> dataNode : dataNodes) {
        if (!(dataNode.getData() instanceof LifecycleData)) continue;
        LifecycleData data = (LifecycleData) dataNode.getData();
        TaskData taskData = new TaskData(SYSTEM_ID, data.getName(), data.getLinkedExternalProjectPath(), null);
        DataNode<TaskData> newNode = new DataNode<>(ProjectKeys.TASK, taskData, dataNode.getParent());
        tasks.add(new TaskNode(externalProjectsView, newNode));
      }
    }
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(AllIcons.Nodes.ConfigFolder);
  }

  @Override
  public String getName() {
    return "Lifecycle";
  }

  @Override
  public boolean isVisible() {
    return super.isVisible() && hasChildren();
  }

  @NotNull
  @Override
  protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
    return tasks;
  }
}
