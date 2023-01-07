package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings

internal class MavenToolWindowFactory : AbstractExternalSystemToolWindowFactory(GMavenConstants.SYSTEM_ID) {

    override fun getSettings(project: Project): AbstractExternalSystemSettings<*, *, *> {
        return MavenSettings.getInstance(project)
    }
}
