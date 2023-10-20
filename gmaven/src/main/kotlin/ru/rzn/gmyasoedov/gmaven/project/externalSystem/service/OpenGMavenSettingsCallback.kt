package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import javax.swing.event.HyperlinkEvent

class OpenGMavenSettingsCallback(private val project: Project) : NotificationListener.Adapter() {

    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, GMavenConstants.SYSTEM_ID.readableName)
    }

    companion object {
        val ID = "#open_gmaven_settings"
    }
}