package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

public class GMavenSystemSettingsControl implements ExternalSystemSettingsControl<MavenSettings> {

  private final GMavenSystemSettingsControlBuilder myBuilder;


  public GMavenSystemSettingsControl(@NotNull MavenSettings settings) {
    this(GMavenSettingsControlProvider.get().getSystemSettingsControlBuilder(settings));
  }

  public GMavenSystemSettingsControl(@NotNull GMavenSystemSettingsControlBuilder builder) {
    myBuilder = builder;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
    myBuilder.fillUi(canvas, indentLevel);
  }

  @Override
  public void showUi(boolean show) {
    myBuilder.showUi(show);
  }

  @Override
  public void reset() {
    myBuilder.reset();
  }

  @Override
  public boolean isModified() {
    return myBuilder.isModified();
  }

  @Override
  public void apply(@NotNull MavenSettings settings) {
    myBuilder.apply(settings);
  }

  @Override
  public boolean validate(@NotNull MavenSettings settings) throws ConfigurationException {
    return myBuilder.validate(settings);
  }

  @Override
  public void disposeUIResources() {
    myBuilder.disposeUIResources();
  }

  @NotNull
  public MavenSettings getInitialSettings() {
    return myBuilder.getInitialSettings();
  }
}
