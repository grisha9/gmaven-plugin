package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
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
}