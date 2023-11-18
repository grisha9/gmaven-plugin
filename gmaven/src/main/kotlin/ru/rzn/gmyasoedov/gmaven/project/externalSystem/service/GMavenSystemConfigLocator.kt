package ru.rzn.gmyasoedov.gmaven.project.externalSystem.service

import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.vfs.VirtualFile
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil.ARTIFACT_ID
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils

class GMavenSystemConfigLocator : ExternalSystemConfigLocator {
    override fun getTargetExternalSystemId() = GMavenConstants.SYSTEM_ID

    override fun adjust(configPath: VirtualFile): VirtualFile? {
        if (!configPath.isDirectory) return configPath

        val result = configPath.findChild(GMavenConstants.POM_XML)
        if (result != null) return result

        val configFileExtensions = MavenUtils.getConfigFileExtensions().toSet()
        for (child in configPath.children) {
            if (child.isDirectory) continue
            val extension = child.extension ?: continue
            if (configFileExtensions.contains(extension)
                && String(child.contentsToByteArray()).contains(ARTIFACT_ID, true)) {
                return child
            }
        }
        return null
    }

    override fun findAll(externalProjectSettings: ExternalProjectSettings) = emptyList<VirtualFile>()
}