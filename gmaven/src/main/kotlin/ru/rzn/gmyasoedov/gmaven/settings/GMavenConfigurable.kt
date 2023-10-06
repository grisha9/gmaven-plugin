package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID

class GMavenConfigurable(project: Project) :
    AbstractExternalSystemConfigurable<MavenProjectSettings, MavenSettingsListener, MavenSettings>(project, SYSTEM_ID) {

    val ID = "reference.settingsdialog.project.gmaven"

    override fun createProjectSettingsControl(settings: MavenProjectSettings) = GMavenProjectSettingsControl(settings)

    override fun createSystemSettingsControl(settings: MavenSettings) = SystemSettingsControlBuilder(settings)

    override fun getId() = ID

    override fun newProjectSettings() = MavenProjectSettings()

    override fun getHelpTopic() = ID
}