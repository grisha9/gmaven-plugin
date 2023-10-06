package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID

class GMavenConfigurable(project: Project) :
    AbstractExternalSystemConfigurable<MavenProjectSettings, MavenSettingsListener, MavenSettings>(project, SYSTEM_ID) {

    private val id = "reference.settingsdialog.project.gmaven"

    override fun createProjectSettingsControl(settings: MavenProjectSettings) = ProjectSettingsControlBuilder(settings)

    override fun createSystemSettingsControl(settings: MavenSettings) = SystemSettingsControlBuilder(settings)

    override fun getId() = id

    override fun newProjectSettings() = MavenProjectSettings()

    override fun getHelpTopic() = id
}