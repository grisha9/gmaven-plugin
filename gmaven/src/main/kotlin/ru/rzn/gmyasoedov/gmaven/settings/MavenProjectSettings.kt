package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings

class MavenProjectSettings : ExternalProjectSettings() {
    var projectBuildFile: String? = null
    var distributionSettings: DistributionSettings = DistributionSettings.getBundled()
    var jdkName: String? = ExternalSystemJdkUtil.USE_PROJECT_JDK
    var vmOptions: String? = null
    var resolveModulePerSourceSet = true
    var offline  = false

    override fun clone(): MavenProjectSettings {
        val result = MavenProjectSettings()
        copyTo(result)
        result.jdkName = jdkName
        result.resolveModulePerSourceSet = resolveModulePerSourceSet
        result.vmOptions = vmOptions
        result.offline = offline
        result.distributionSettings = distributionSettings
        result.projectBuildFile = projectBuildFile;
        return result
    }
}