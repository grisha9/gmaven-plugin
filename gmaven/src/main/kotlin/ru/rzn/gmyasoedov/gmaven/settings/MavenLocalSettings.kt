package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID

@State(name = "MavenLocalSettings", storages = [Storage(StoragePathMacros.CACHE_FILE)])
class MavenLocalSettings(project: Project) : AbstractExternalSystemLocalSettings<MavenLocalSettings.GMavenLocalState>(
    SYSTEM_ID, project, GMavenLocalState()
) {
    class GMavenLocalState : State()
}