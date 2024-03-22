package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.settings.DelegatingExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;

public class DelegatingMavenSettingsListenerAdapter
        extends DelegatingExternalSystemSettingsListener<MavenProjectSettings>
        implements MavenSettingsListener {

    public DelegatingMavenSettingsListenerAdapter(ExternalSystemSettingsListener<MavenProjectSettings> delegate) {
        super(delegate);
    }
}
