
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle;

public class SystemSettingsControlBuilder implements GMavenSystemSettingsControlBuilder {

    @NotNull
    private final MavenSettings myInitialSettings;

    @Nullable
    private JBCheckBox offlineCheckBox;
    private JBCheckBox skipTestsCheckBox;

    public SystemSettingsControlBuilder(@NotNull MavenSettings initialSettings) {
        myInitialSettings = initialSettings;
    }

    @Override
    public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
        offlineCheckBox = new JBCheckBox(GBundle.message("gmaven.settings.system.offline"));
        skipTestsCheckBox = new JBCheckBox(GBundle.message("gmaven.settings.system.skip.tests"));

        canvas.add(offlineCheckBox, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
        canvas.add(skipTestsCheckBox, ExternalSystemUiUtil.getFillLineConstraints(0));
    }

    @Override
    public void showUi(boolean show) {
        ExternalSystemUiUtil.showUi(this, show);
    }

    @Override
    public void reset() {
        if (offlineCheckBox != null) {
            offlineCheckBox.setSelected(myInitialSettings.isOfflineMode());
        }
        if (skipTestsCheckBox != null) {
            skipTestsCheckBox.setSelected(myInitialSettings.isSkipTests());
        }
    }

    @Override
    public boolean isModified() {
        if (offlineCheckBox != null && offlineCheckBox.isSelected() != myInitialSettings.isOfflineMode()) {
            return true;
        }
        if (skipTestsCheckBox != null && skipTestsCheckBox.isSelected() != myInitialSettings.isSkipTests()) {
            return true;
        }
        return false;
    }

    @Override
    public void apply(@NotNull MavenSettings settings) {
        if (offlineCheckBox != null) {
            settings.setOfflineMode(offlineCheckBox.isSelected());
        }
        if (skipTestsCheckBox != null) {
            settings.setSkipTests(skipTestsCheckBox.isSelected());
        }
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
}
