// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;

public class GMavenConfigurable extends
        AbstractExternalSystemConfigurable<MavenProjectSettings, MavenSettingsListener, MavenSettings> {

    public static final String DISPLAY_NAME = GMavenConstants.SYSTEM_ID.getReadableName();
    public static final String ID = "reference.settingsdialog.project.gmaven";
    @NonNls
    public static final String HELP_TOPIC = ID;

    public GMavenConfigurable(@NotNull Project project) {
        super(project, GMavenConstants.SYSTEM_ID);
    }

    @NotNull
    @Override
    protected ExternalSystemSettingsControl<MavenProjectSettings> createProjectSettingsControl(
            @NotNull MavenProjectSettings settings) {
        return new GMavenProjectSettingsControl(settings);
    }

    @Nullable
    @Override
    protected ExternalSystemSettingsControl<MavenSettings> createSystemSettingsControl(@NotNull MavenSettings settings) {
        return new GMavenSystemSettingsControl(settings);
    }

    @NotNull
    @Override
    protected MavenProjectSettings newProjectSettings() {
        return new MavenProjectSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public String getHelpTopic() {
        return HELP_TOPIC;
    }
}
