package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import org.jetbrains.annotations.NotNull;

public interface GMavenSystemSettingsControlBuilder {
  void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel);

  void showUi(boolean show);

  void reset();

  boolean isModified();

  void apply(@NotNull MavenSettings settings);

  boolean validate(@NotNull MavenSettings settings);

  void disposeUIResources();

  @NotNull
  MavenSettings getInitialSettings();
}
