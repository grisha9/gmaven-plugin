package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID

const val ID = "reference.settingsdialog.project.gmaven"

class GMavenConfigurable(project: Project) :
    AbstractExternalSystemConfigurable<MavenProjectSettings, MavenSettingsListener, MavenSettings>(project, SYSTEM_ID) {

    override fun getId() = ID

    override fun getHelpTopic() = ID

    override fun newProjectSettings() = MavenProjectSettings()

    override fun createSystemSettingsControl(settings: MavenSettings) = SystemSettingsControlBuilder(settings)

    override fun createProjectSettingsControl(settings: MavenProjectSettings) = ProjectSettingsControl(project, settings)
}