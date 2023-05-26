
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SystemSettingsControlBuilder implements GMavenSystemSettingsControlBuilder {

  @NotNull
  private final MavenSettings myInitialSettings;

  // Used by reflection at showUi() and disposeUiResources()
  @SuppressWarnings("FieldCanBeLocal")
  @Nullable
  private JBLabel myServiceDirectoryLabel;
  private JBLabel myServiceDirectoryHint;


  @Nullable
  private JBTextField myGradleVmOptionsField;
  List<Component> myGradleVmOptionsComponents = new ArrayList<>();
  private boolean dropVmOptions;

  @Nullable
  private JBCheckBox myGenerateImlFilesCheckBox;
  private JBLabel myGenerateImlFilesHint;
  private boolean dropStoreExternallyCheckBox;

  public SystemSettingsControlBuilder(@NotNull MavenSettings initialSettings) {
    myInitialSettings = initialSettings;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {


  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }

  @Override
  public void reset() {
    if (myGenerateImlFilesCheckBox != null) {
      myGenerateImlFilesCheckBox.setSelected(!myInitialSettings.getStoreProjectFilesExternally());
    }
  }

  @Override
  public boolean isModified() {


    return false;
  }

  @Override
  public void apply(@NotNull MavenSettings settings) {

  }

  @Override
  public boolean validate(@NotNull MavenSettings settings) {
    return true;
  }

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @NotNull
  @Override
  public MavenSettings getInitialSettings() {
    return myInitialSettings;
  }

  public SystemSettingsControlBuilder dropStoreExternallyCheckBox() {
    dropStoreExternallyCheckBox = true;
    return this;
  }

}
