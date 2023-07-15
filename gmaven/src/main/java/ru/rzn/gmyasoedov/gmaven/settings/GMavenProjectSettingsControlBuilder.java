package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface GMavenProjectSettingsControlBuilder {

  /**
   * Hides/shows components added by the current control}.
   * @param show  flag which indicates if current control' components should be visible
   */
  void showUi(boolean show);

  /**
   * get initial settings
   * @return
   */
  MavenProjectSettings getInitialSettings();

  /**
   * Add Gradle JDK component to the panel
   */
  GMavenProjectSettingsControlBuilder addGradleJdkComponents(JPanel content, int indentLevel);

  /**
   * Add Gradle distribution chooser component to the panel
   */
  GMavenProjectSettingsControlBuilder addGradleChooserComponents(JPanel content, int indentLevel);

  boolean validate(MavenProjectSettings settings) throws ConfigurationException;

  void apply(MavenProjectSettings settings);

  /**
   * check if something was changed against initial settings
   * @return
   */
  boolean isModified();

  default void reset(@Nullable Project project,
                     MavenProjectSettings settings,
                     boolean isDefaultModuleCreation,
                     @Nullable WizardContext wizardContext) {
  }

  void createAndFillControls(PaintAwarePanel content, int indentLevel);

  void update(String linkedProjectPath, MavenProjectSettings settings, boolean isDefaultModuleCreation);

  void disposeUIResources();
}
