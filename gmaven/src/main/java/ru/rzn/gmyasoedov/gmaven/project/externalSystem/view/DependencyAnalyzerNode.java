package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Order(ExternalSystemNode.BUILTIN_DEPENDENCIES_DATA_NODE_ORDER)
public class DependencyAnalyzerNode extends ExternalSystemNode<Object> {

    @SuppressWarnings("unchecked")
    public DependencyAnalyzerNode(ExternalProjectsView externalProjectsView) {
        super(externalProjectsView, null, null);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(AllIcons.Actions.DependencyAnalyzer);
    }

    @Override
    public String getName() {
        return "Dependencies";
    }

    @Nullable
    @Override
    protected String getActionId() {
        return "GMaven.ToolbarDependencyAnalyzer";
    }
}
