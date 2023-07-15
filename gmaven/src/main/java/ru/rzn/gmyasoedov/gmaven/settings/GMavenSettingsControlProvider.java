package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;

public class GMavenSettingsControlProvider {

  public String getPlatformPrefix() {
    return PlatformUtils.isIntelliJ() ? PlatformUtils.getPlatformPrefix() : PlatformUtils.IDEA_CE_PREFIX;
  }

  public GMavenSystemSettingsControlBuilder getSystemSettingsControlBuilder(@NotNull MavenSettings initialSettings) {
    return new SystemSettingsControlBuilder(initialSettings);
  }

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
