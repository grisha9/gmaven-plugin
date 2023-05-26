package ru.rzn.gmyasoedov.gmaven.project.externalSystem.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData;

public class ProfileNode extends ExternalSystemNode<ProfileData> {
    private final ProfileData profileData;

    public ProfileNode(@NotNull ExternalProjectsView externalProjectsView, @NotNull DataNode<ProfileData> dataNode) {
        super(externalProjectsView, null, dataNode);
        profileData = dataNode.getData();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        super.update(presentation);
        String state = profileData.getState();
        if (ProfileData.SimpleProfile.ACTIVE.name().equals(state)) {
            presentation.setIcon(AllIcons.Diff.GutterCheckBoxSelected);
        } else if (ProfileData.SimpleProfile.INACTIVE.name().equals(state)) {
            presentation.setIcon(AllIcons.Diff.GutterCheckBox);
        } else {
            presentation.setIcon(AllIcons.Diff.GutterCheckBoxIndeterminate);
        }
    }

    @Override
    public @NlsSafe String getName() {
        return super.getName();
    }

    @Nullable
    @Override
    protected String getActionId() {
        return "GMaven.ExternalSystem.ChangeProfile";
    }

    @Override
    public boolean isAlwaysLeaf() {
        return true;
    }
}
