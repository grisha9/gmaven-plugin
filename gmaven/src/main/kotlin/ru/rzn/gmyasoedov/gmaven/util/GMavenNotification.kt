package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory.ERROR
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle.message

object GMavenNotification {
    fun createNotificationDA(content: String, type: NotificationType) {
        createNotification(message("gmaven.dependency.tree.title"), content, type)
    }

    fun createNotification(
        title: String, content: String,
        type: NotificationType,
        actions: List<AnAction> = emptyList()
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GMavenConstants.SYSTEM_ID.readableName)
            ?.let { performNotification(it, title, content, type, actions) }
    }

    private fun performNotification(
        it: NotificationGroup, title: String, content: String,
        type: NotificationType, actions: List<AnAction> = emptyList()
    ) {
        ApplicationManager.getApplication().invokeLater {
            val notification = it.createNotification(title, content, type)
            actions.forEach { notification.addAction(it) }
            notification.notify(null)
        }
    }

    fun errorExternalSystemNotification(
        title: String, message: String, project: Project
    ) {
        val notification = NotificationData(title, message, ERROR, NotificationSource.TASK_EXECUTION)
        notification.isBalloonNotification = true
        ExternalSystemNotificationManager.getInstance(project)
            .showNotification(GMavenConstants.SYSTEM_ID, notification)
    }
}