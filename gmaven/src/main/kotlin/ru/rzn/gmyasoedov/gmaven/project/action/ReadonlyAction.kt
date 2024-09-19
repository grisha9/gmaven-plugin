package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.registry.Registry
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle

class ReadonlyAction : GMavenToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        return Registry.`is`("gmaven.import.readonly")
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        Registry.get("gmaven.import.readonly").setValue(!Registry.`is`("gmaven.import.readonly"))
    }

    init {
        templatePresentation.icon = AllIcons.Ide.Readonly
        templatePresentation.text = GBundle.message("gmaven.settings.system.readonly.tooltip")
    }
}