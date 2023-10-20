package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.LocationAwareExternalSystemException
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationExtension
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.callback.OpenExternalSystemSettingsCallback
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class GMavenNotificationExtension : ExternalSystemNotificationExtension {

    override fun getTargetExternalSystemId() = GMavenConstants.SYSTEM_ID

    override fun customize(notificationData: NotificationData, project: Project, error: Throwable?) {
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
}