package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.BUILTIN_TASKS_DATA_NODE_ORDER;

@Order(BUILTIN_TASKS_DATA_NODE_ORDER - 2)
public class ProfileNodes extends ExternalSystemNode<AbstractExternalEntityData> {

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
        presentation.setIcon(AllIcons.Nodes.ConfigFolder);
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

    public List<ProfileNode> getProfiles() {
        return List.copyOf(profiles);
    }

    @Override
    public @Nullable AbstractExternalEntityData getData() {
        return profiles.isEmpty() ? null : profiles.get(0).getData();
    }

    @Override
    protected @Nullable @NonNls String getMenuId() {
        return "ExternalSystemView.GMaven.ProfileMenu";
    }
}
