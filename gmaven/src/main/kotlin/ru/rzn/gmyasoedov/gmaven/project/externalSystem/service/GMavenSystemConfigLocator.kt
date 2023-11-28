package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleDataService

class GMavenSystemConfigLocator : ExternalSystemConfigLocator {
    override fun getTargetExternalSystemId() = GMavenConstants.SYSTEM_ID

    override fun adjust(configPath: VirtualFile): VirtualFile? {
        if (!configPath.isDirectory) return configPath

        val configPaths = CachedModuleDataService.getCurrentData().allConfigPaths
        for (child in configPath.children) {
            if (child.isDirectory) continue
            val nioPath = child.toNioPathOrNull()?.toString() ?: continue
            if (configPaths.contains(nioPath)) return child
        }
        return null
    }

    override fun findAll(externalProjectSettings: ExternalProjectSettings) = emptyList<VirtualFile>()
}