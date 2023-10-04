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
    var resolveModulePerSourceSet = false
    var nonRecursive  = false
    var useWholeProjectContext = true
    var showPluginNodes = true
    var useMvndForTasks = false
    var snapshotUpdateType = SnapshotUpdateType.DEFAULT
    var outputLevel = OutputLevelType.DEFAULT
    var threadCount: String? = null
    var arguments: String? = null
    var argumentsImport: String? = null

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
        result.useWholeProjectContext = useWholeProjectContext
        result.arguments = arguments
        result.argumentsImport = argumentsImport
        result.useMvndForTasks = useMvndForTasks
        return result
    }
}