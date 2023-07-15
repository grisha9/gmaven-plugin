package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.settings.DelegatingExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelegatingMavenSettingsListenerAdapter
        extends DelegatingExternalSystemSettingsListener<MavenProjectSettings>
        implements MavenSettingsListener {

    public DelegatingMavenSettingsListenerAdapter(@NotNull ExternalSystemSettingsListener<MavenProjectSettings> delegate) {
        super(delegate);
    }


    @Override
    public void onMavenHomeChange(@Nullable String oldPath, @Nullable String newPath, @NotNull String linkedProjectPath) {
    }

    @Override
    public void onServiceDirectoryPathChange(@Nullable String oldPath, @Nullable String newPath) {

    }

    @Override
    public void onVmOptionsChange(@Nullable String oldOptions, @Nullable String newOptions) {

    }
}
