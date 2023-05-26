package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import icons.GMavenIcons;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER - 2)
public class ProfileNodes extends ExternalSystemNode<Object> {

    private final List<ProfileNode> profiles;

    @SuppressWarnings("unchecked")
    public ProfileNodes(ExternalProjectsView externalProjectsView, final Collection<? extends DataNode<?>> dataNodes) {
        super(externalProjectsView, null, null);
        profiles = new ArrayList<>(dataNodes == null ? 0 : dataNodes.size());
        if (dataNodes != null && !dataNodes.isEmpty()) {
            for (DataNode<?> dataNode : dataNodes) {
                if (!(dataNode.getData() instanceof ProfileData)) continue;
                profiles.add(new ProfileNode(externalProjectsView, (DataNode<ProfileData>) dataNode));
            }
        }
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(GMavenIcons.ProfilesClosed);
    }

    @Override
    public String getName() {
        return "Profiles";
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && hasChildren();
    }

    @NotNull
    @Override
    protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
        return profiles;
    }
}
