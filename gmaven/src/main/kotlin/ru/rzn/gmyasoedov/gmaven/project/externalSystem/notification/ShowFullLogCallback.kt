package ru.rzn.gmyasoedov.gmaven.project.externalSystem.notification

import com.intellij.build.BuildContentManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.event.HyperlinkEvent

class ShowFullLogCallback(private val project: Project) : NotificationListener.Adapter() {

    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        val buildToolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(BuildContentManager.TOOL_WINDOW_ID) ?: return
        if (!buildToolWindow.isActive) return
        val contentManager = buildToolWindow.contentManager
        val contents = contentManager.contents
        if (contents.isEmpty()) return
        val content = contents.find { it.tabName.equals("sync", true) } ?: return
        println(content)
        //MultipleBuildsView BuildTreeConsoleView.onEvent
    }

    companion object {
        const val ID = "#gmaven_show_full_log"
    }
}