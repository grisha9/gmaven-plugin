@file:JvmName("MavenJvmResolutionUtil")

package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings

fun updateMavenJdk(project: Project, externalProjectPath: String) {
    val settings = MavenSettings.getInstance(project)
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
    val jdkName = projectSettings.jdkName ?: return
    val projectRootManager = ProjectRootManager.getInstance(project)

    val projectSdk = projectRootManager.projectSdk ?: return
    if (projectSdk.name != jdkName) return
    projectSettings.jdkName = ExternalSystemJdkUtil.USE_PROJECT_JDK
}