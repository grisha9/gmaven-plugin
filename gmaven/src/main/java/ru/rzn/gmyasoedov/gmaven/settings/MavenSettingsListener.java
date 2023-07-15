package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MavenSettingsListener extends ExternalSystemSettingsListener<MavenProjectSettings> {
  Topic<MavenSettingsListener> TOPIC = new Topic<>(MavenSettingsListener.class, Topic.BroadcastDirection.NONE);

  void onMavenHomeChange(@Nullable String oldPath, @Nullable String newPath, @NotNull String linkedProjectPath);

  /*void onDistributionTypeChange(DistributionType currentValue, @NotNull String linkedProjectPath);*/

  void onServiceDirectoryPathChange(@Nullable String oldPath, @Nullable String newPath);

  void onVmOptionsChange(@Nullable String oldOptions, @Nullable String newOptions);

}
