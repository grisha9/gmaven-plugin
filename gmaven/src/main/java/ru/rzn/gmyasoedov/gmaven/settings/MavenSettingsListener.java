package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.util.messages.Topic;

public interface MavenSettingsListener extends ExternalSystemSettingsListener<MavenProjectSettings> {
  Topic<MavenSettingsListener> TOPIC = new Topic<>(MavenSettingsListener.class, Topic.BroadcastDirection.NONE);
}
