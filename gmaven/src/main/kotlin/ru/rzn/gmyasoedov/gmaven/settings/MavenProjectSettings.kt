package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.OutputLevelType
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.SnapshotUpdateType

class MavenProjectSettings : ExternalProjectSettings() {
    var projectBuildFile: String? = null
    var distributionSettings: DistributionSettings = DistributionSettings.getBundled()
    var jdkName: String? = ExternalSystemJdkUtil.USE_PROJECT_JDK
    var vmOptions: String? = null
    var resolveModulePerSourceSet = true
    var nonRecursive  = false
    var showPluginNodes = true
    var incrementalSync = false
    var snapshotUpdateType = SnapshotUpdateType.DEFAULT
    var outputLevel = OutputLevelType.DEFAULT
    var threadCount: String? = null
    var arguments: String? = null
    var argumentsImport: String? = null
    var localRepositoryPath: String? = null

    override fun clone(): MavenProjectSettings {
        val result = MavenProjectSettings()
        copyTo(result)
        result.jdkName = jdkName
        result.resolveModulePerSourceSet = resolveModulePerSourceSet
        result.vmOptions = vmOptions
        result.nonRecursive = nonRecursive
        result.distributionSettings = distributionSettings
        result.projectBuildFile = projectBuildFile
        result.outputLevel = outputLevel
        result.snapshotUpdateType = snapshotUpdateType
        result.showPluginNodes = showPluginNodes
        result.threadCount = threadCount
        result.arguments = arguments
        result.argumentsImport = argumentsImport
        result.localRepositoryPath = localRepositoryPath
        result.incrementalSync = incrementalSync
        return result
    }
}