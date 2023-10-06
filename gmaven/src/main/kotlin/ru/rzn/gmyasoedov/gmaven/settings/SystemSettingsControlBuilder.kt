package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.ui.components.JBCheckBox
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle

class SystemSettingsControlBuilder(private val initialSettings: MavenSettings) :
    ExternalSystemSettingsControl<MavenSettings> {

    private var offlineCheckBox: JBCheckBox? = null
    private var skipTestsCheckBox: JBCheckBox? = null

    override fun fillUi(canvas: PaintAwarePanel, indentLevel: Int) {
        offlineCheckBox = JBCheckBox(GBundle.message("gmaven.settings.system.offline"))
        skipTestsCheckBox = JBCheckBox(GBundle.message("gmaven.settings.system.skip.tests"))
        offlineCheckBox?.apply { canvas.add(this, ExternalSystemUiUtil.getLabelConstraints(indentLevel)) }
        skipTestsCheckBox?.apply { canvas.add(this, ExternalSystemUiUtil.getLabelConstraints(indentLevel)) }
    }

    override fun showUi(show: Boolean) {
        ExternalSystemUiUtil.showUi(this, show)
    }

    override fun reset() {
        offlineCheckBox?.also { it.setSelected(initialSettings.isOfflineMode) }
        skipTestsCheckBox?.also { it.setSelected(initialSettings.isSkipTests) }
    }

    override fun isModified(): Boolean {
        if (offlineCheckBox != null && offlineCheckBox!!.isSelected != initialSettings.isOfflineMode) {
            return true
        }
        if (skipTestsCheckBox != null && skipTestsCheckBox!!.isSelected != initialSettings.isSkipTests) {
            return true
        }
        return false
    }

    override fun apply(settings: MavenSettings) {
        offlineCheckBox?.also { settings.isOfflineMode = it.isSelected }
        skipTestsCheckBox?.also { settings.isSkipTests = it.isSelected }
    }

    override fun validate(settings: MavenSettings): Boolean {
        return true
    }

    override fun disposeUIResources() {
        ExternalSystemUiUtil.disposeUi(this)
    }
}