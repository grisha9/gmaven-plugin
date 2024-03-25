package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.LocationAwareExternalSystemException
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationExtension
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.callback.OpenExternalSystemSettingsCallback
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog

class GMavenNotificationExtension : ExternalSystemNotificationExtension {

    override fun getTargetExternalSystemId() = GMavenConstants.SYSTEM_ID

    override fun customize(
        notificationData: NotificationData, project: Project,  error: Throwable?
    ) {
        printError(error)
        val e = (if (error is ExternalSystemException) error else null) ?: return
        for (fix in e.quickFixes) {
            if (OpenGMavenSettingsCallback.ID == fix) {
                notificationData.setListener(OpenGMavenSettingsCallback.ID, OpenGMavenSettingsCallback(project))
            } else if (OpenExternalSystemSettingsCallback.ID == fix) {
                val linkedProjectPath =
                    if (e is LocationAwareExternalSystemException) e.filePath else null
                notificationData.setListener(
                    OpenExternalSystemSettingsCallback.ID,
                    OpenExternalSystemSettingsCallback(project, GMavenConstants.SYSTEM_ID, linkedProjectPath)
                )
            }
        }
    }

    private fun printError(error: Throwable?) {
        if (!Registry.`is`("gmaven.show.all.errors")) return
        if (error?.stackTrace?.any { it.className.contains("ru.rzn.gmyasoedov.gmaven") } == true) {
            MavenLog.LOG.error(error)
        }
    }
}