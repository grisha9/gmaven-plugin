@file:JvmName("WizardUtils")

package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.gmaven.settings.DistributionType
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import kotlin.io.path.absolutePathString

fun createMavenProjectSettings(projectFile: VirtualFile, project: Project): MavenProjectSettings {
    val projectDirectory = if (projectFile.isDirectory) projectFile else projectFile.parent
    val settings = MavenProjectSettings()
    settings.distributionSettings = getDistributionSettings(settings, projectDirectory)
    //only canonicalPath work!!!
    settings.externalProjectPath = projectDirectory.canonicalPath
    settings.projectBuildFile = if (!projectFile.isDirectory) projectFile.toNioPath().absolutePathString() else null
    settings.jdkName = (MavenUtils.suggestProjectSdk()
        ?: ExternalSystemJdkUtil.getJdk(project, ExternalSystemJdkUtil.USE_INTERNAL_JAVA))?.name
    return settings;
}

private fun getDistributionSettings(
    settings: MavenProjectSettings,
    projectDirectory: VirtualFile
): DistributionSettings {
    if (settings.distributionSettings.type == DistributionType.CUSTOM) return settings.distributionSettings
    if (MvnDotProperties.isWrapperExist(projectDirectory)) {
        return DistributionSettings(DistributionType.WRAPPER, null, null)
    }

    val mavenHome = MavenUtils.resolveMavenHome()
    if (mavenHome != null) return DistributionSettings.getLocal(mavenHome.toPath())

    return settings.distributionSettings
}