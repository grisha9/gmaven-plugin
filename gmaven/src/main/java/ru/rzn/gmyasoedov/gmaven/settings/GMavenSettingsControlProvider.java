package ru.rzn.gmyasoedov.gmaven.settings;

import org.jetbrains.annotations.NotNull;

public class GMavenSettingsControlProvider {

  public ProjectSettingsControlBuilder getProjectSettingsControlBuilder(@NotNull MavenProjectSettings initialSettings) {
    return new ProjectSettingsControlBuilder(initialSettings);
            // Hide bundled distribution option for a while
            //.dropUseBundledDistributionButton(); todo???
  }
  @NotNull
  public static GMavenSettingsControlProvider get() {
    return new GMavenSettingsControlProvider();
  }
}
