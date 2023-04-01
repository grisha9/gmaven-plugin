package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.getBundledDistributionUrl
import ru.rzn.gmyasoedov.gmaven.settings.DistributionType.BUNDLED

class MavenProjectSettings : ExternalProjectSettings() {
    var projectDirectory: String? = null
   // var mavenHome: String? = null
    var distributionSettings: DistributionSettings = DistributionSettings(BUNDLED, url = getBundledDistributionUrl())
    var jdkName: String? = ExternalSystemJdkUtil.USE_PROJECT_JDK
    var vmOptions: String? = null
    var resolveModulePerSourceSet = true
    var offline  = false

    override fun clone(): MavenProjectSettings {
        val result = MavenProjectSettings()
        copyTo(result)
     //   result.mavenHome = mavenHome
        result.jdkName = jdkName
        result.resolveModulePerSourceSet = resolveModulePerSourceSet
        result.vmOptions = vmOptions
        result.offline = offline
        result.distributionSettings = distributionSettings
        return result
    }
}