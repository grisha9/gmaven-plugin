// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.settings.LocationSettingType;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

public class ProjectSettingsControlBuilder implements GMavenProjectSettingsControlBuilder {
  private static final long BALLOON_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
  private static final String HIDDEN_KEY = "hidden";
  @NotNull
  private final MavenProjectSettings myInitialSettings;
  @NotNull
  private final Alarm myAlarm = new Alarm();
  /**
   * The target {@link Project} reference of the UI control.
   * It can be the current project of the settings UI configurable (see {@org.jetbrains.plugins.gradle.service.settings.GradleConfigurable}),
   * or the target project from the wizard context.
   */
  @NotNull
  private final Ref<Project> myProjectRef = Ref.create();
  @NotNull
  private final Disposable myProjectRefDisposable = () -> myProjectRef.set(null);
  @Nullable JBLabel myGradleDistributionHint;
  @NotNull
  private LocationSettingType myGradleHomeSettingType = LocationSettingType.UNKNOWN;
  private boolean myShowBalloonIfNecessary;
  @Nullable
  @SuppressWarnings({"unused", "RedundantSuppression"}) // used by ExternalSystemUiUtil.showUi to show/hide the component via reflection
  private JPanel myGradlePanel;

  public ProjectSettingsControlBuilder(@NotNull MavenProjectSettings initialSettings) {
    myInitialSettings = initialSettings;
  }

  public ProjectSettingsControlBuilder dropGradleJdkComponents() {

    return this;
  }

  public ProjectSettingsControlBuilder dropUseWrapperButton() {

    return this;
  }

  public ProjectSettingsControlBuilder dropCustomizableWrapperButton() {

    return this;
  }

  public ProjectSettingsControlBuilder dropUseLocalDistributionButton() {

    return this;
  }

  public ProjectSettingsControlBuilder dropUseBundledDistributionButton() {

    return this;
  }

  public ProjectSettingsControlBuilder dropResolveModulePerSourceSetCheckBox() {

    return this;
  }

  public ProjectSettingsControlBuilder dropResolveExternalAnnotationsCheckBox() {

    return this;
  }

  public ProjectSettingsControlBuilder dropDelegateBuildCombobox() {

    return this;
  }

  public ProjectSettingsControlBuilder dropTestRunnerCombobox() {

    return this;
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);

    if (show) {
      // some controls need to remain hidden depending on the selection
      // also error notifications should be shown

    }
  }

  @Override
  @NotNull
  public MavenProjectSettings getInitialSettings() {
    return myInitialSettings;
  }

  @Override
  public GMavenProjectSettingsControlBuilder addGradleJdkComponents(JPanel content, int indentLevel) {
    return null;
  }

  @Override
  public GMavenProjectSettingsControlBuilder addGradleChooserComponents(JPanel content, int indentLevel) {
    return null;
  }

  @Override
  public boolean validate(MavenProjectSettings settings) throws ConfigurationException {
    return false;
  }

  @Override
  public void apply(MavenProjectSettings settings) {

  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void reset(@Nullable Project project, MavenProjectSettings settings, boolean isDefaultModuleCreation) {

  }

  @Override
  public void reset(@Nullable Project project, MavenProjectSettings settings, boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext) {
    GMavenProjectSettingsControlBuilder.super.reset(project, settings, isDefaultModuleCreation, wizardContext);
  }

  @Override
  public void createAndFillControls(PaintAwarePanel content, int indentLevel) {

  }

  @Override
  public void update(String linkedProjectPath, MavenProjectSettings settings, boolean isDefaultModuleCreation) {

  }

  @Override
  public void disposeUIResources() {

  }


}
