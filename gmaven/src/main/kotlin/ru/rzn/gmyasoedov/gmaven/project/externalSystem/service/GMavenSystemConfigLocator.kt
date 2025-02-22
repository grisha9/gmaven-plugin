package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.util.CachedDataHolder
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleDataService

class GMavenSystemConfigLocator : ExternalSystemConfigLocator {
    override fun getTargetExternalSystemId() = GMavenConstants.SYSTEM_ID

    override fun adjust(configPath: VirtualFile): VirtualFile? {
        if (!configPath.isDirectory) return configPath

        val cachedDataHolder = cachedDataHolder()
        for (child in configPath.children) {
            if (child.isDirectory) continue
            val nioPath = child.toNioPathOrNull()?.toString() ?: continue
            if (cachedDataHolder.isConfigPath(nioPath)) return child
        }
        return null
    }

    private fun cachedDataHolder(): CachedDataHolder {
        val currentData = CachedModuleDataService.getCurrentData()
        if (currentData.activeConfigPaths.isEmpty()) {
            val project = gutil.findCurrentProject() ?: return currentData
            val dataHolder = CachedModuleDataService.getDataHolder(project)
            if (dataHolder.activeConfigPaths.isEmpty()) {
                CachedModuleDataService.invalidate()
                return CachedModuleDataService.getDataHolder(project)
            }
            return dataHolder
        }
        return currentData
    }

    override fun findAll(externalProjectSettings: ExternalProjectSettings) = emptyList<VirtualFile>()
}