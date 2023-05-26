package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 */
public class GMavenProjectSettingsControl extends AbstractExternalProjectSettingsControl<MavenProjectSettings> {
  private final ProjectSettingsControlBuilder myBuilder;

  public GMavenProjectSettingsControl(@NotNull MavenProjectSettings initialSettings) {
    this(GMavenSettingsControlProvider.get().getProjectSettingsControlBuilder(initialSettings));
  }

  public GMavenProjectSettingsControl(@NotNull ProjectSettingsControlBuilder builder) {
    super(null, builder.getInitialSettings());
    myBuilder = builder;
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    myBuilder.createAndFillControls(content, indentLevel);
  }

  @Override
  public boolean validate(@NotNull MavenProjectSettings settings) throws ConfigurationException {
    return myBuilder.validate(settings);
  }

  @Override
  protected void applyExtraSettings(@NotNull MavenProjectSettings settings) {
    myBuilder.apply(settings);
  }

  @Override
  protected void updateInitialExtraSettings() {
    myBuilder.apply(getInitialSettings());
  }

  @Override
  protected boolean isExtraSettingModified() {
    return myBuilder.isModified();
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    resetExtraSettings(isDefaultModuleCreation, null);
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext) {
    myBuilder.reset(getProject(), getInitialSettings(), isDefaultModuleCreation, wizardContext);
  }

  public void update(@Nullable String linkedProjectPath, boolean isDefaultModuleCreation) {
    myBuilder.update(linkedProjectPath, getInitialSettings(), isDefaultModuleCreation);
  }

  @Override
  public void showUi(boolean show) {
    super.showUi(show);
    myBuilder.showUi(show);
  }

  @Override
  public void setCurrentProject(@Nullable Project project) {
    super.setCurrentProject(project);
  }

  @Override
  public void disposeUIResources() {
    super.disposeUIResources();
    myBuilder.disposeUIResources();
  }

  @Nullable
  @Override
  public String getHelpId() {
    return "Import_from_Gradle_Page_1";
  }
}
