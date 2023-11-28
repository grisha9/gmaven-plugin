package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.vfs.VirtualFile
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleData
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils

class GMavenSystemConfigLocator : ExternalSystemConfigLocator {
    override fun getTargetExternalSystemId() = GMavenConstants.SYSTEM_ID

    override fun adjust(configPath: VirtualFile): VirtualFile? {
        if (!configPath.isDirectory) return configPath

        val configPaths = CachedModuleData.getAllConfigPaths()
        for (child in configPath.children) {
            if (child.isDirectory) continue
            val nioPath = MavenUtils.toNioPathOrNull(child)?.toString() ?: continue
            if (configPaths.contains(nioPath)) return child
        }
        return null
    }

    override fun findAll(externalProjectSettings: ExternalProjectSettings) = emptyList<VirtualFile>()
}