@file:JvmName("MavenJvmResolutionUtil")
package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.Id
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import java.nio.file.Path
import java.nio.file.Paths

private data class MavenJvmProviderId(val projectSettings: MavenProjectSettings) : Id

fun getMavenJvmLookupProvider(project: Project, projectSettings: MavenProjectSettings) =
  SdkLookupProvider.getInstance(project, MavenJvmProviderId(projectSettings))

fun setupJvm(project: Project, projectSettings: MavenProjectSettings) {
  val resolutionContext = MavenJvmResolutionContext(project, Paths.get(projectSettings.externalProjectPath))
  projectSettings.jdkName = resolutionContext.findMavenJvm()
  if (projectSettings.jdkName != null) {
    return
  }

  when {
    resolutionContext.canUseProjectSdk() -> projectSettings.jdkName = ExternalSystemJdkUtil.USE_PROJECT_JDK
    //resolutionContext.canUseGradleJavaHomeJdk() -> projectSettings.gradleJvm = USE_GRADLE_JAVA_HOME
    //resolutionContext.canUseJavaHomeJdk() -> projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_JAVA_HOME
    else -> getMavenJvmLookupProvider(project, projectSettings)
      .newLookupBuilder()
      .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
      .withSdkHomeFilter { ExternalSystemJdkUtil.isValidJdk(it) }
      .onSdkNameResolved { sdk ->
        /* We have two types of sdk resolving:
         *  1. Download sdk manually
         *    a. by download action from SdkComboBox
         *    b. by sdk downloader
         *    c. by action that detects incorrect project sdk
         *  2. Lookup sdk (search in fs, download and etc)
         *    a. search in fs, search in sdk table and etc
         *    b. download
         *
         * All download actions generates fake (invalid) sdk and puts it to jdk table.
         * This code allows to avoid some irregular conflicts
         * For example: strange duplications in SdkComboBox or unexpected modifications of gradleJvm
         */
        val fakeSdk = sdk?.let(::findRegisteredSdk)
        if (fakeSdk != null && projectSettings.jdkName == null) {
          projectSettings.jdkName = fakeSdk.name
        }
      }
      .onSdkResolved { sdk ->
        if (projectSettings.jdkName == null) {
          projectSettings.jdkName = sdk?.name
        }
      }
      .executeLookup()
  }
}

fun updateMavenJdk(project: Project, externalProjectPath: String) {
  val settings = MavenSettings.getInstance(project)
  val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
  val jdkName = projectSettings.jdkName ?: return
  val projectRootManager = ProjectRootManager.getInstance(project)

  val projectSdk = projectRootManager.projectSdk ?: return
  if (projectSdk.name != jdkName) return
  projectSettings.jdkName = ExternalSystemJdkUtil.USE_PROJECT_JDK
}

private class MavenJvmResolutionContext(
  val project: Project,
  val externalProjectPath: Path
)

private fun MavenJvmResolutionContext.findMavenJvm(): String? {
  val settings = MavenSettings.getInstance(project)
  return settings.linkedProjectsSettings.asSequence()
    .mapNotNull { it.jdkName }
    .firstOrNull()
}

private fun MavenJvmResolutionContext.canUseProjectSdk(): Boolean {
  val projectJdk = resolveProjectJdk(project) ?: return false
  return ExternalSystemJdkUtil.isValidJdk(projectJdk)
}

private fun resolveProjectJdk(project: Project): Sdk? {
  val projectRootManager = ProjectRootManager.getInstance(project)
  val projectSdk = projectRootManager.projectSdk ?: return null
  return ExternalSystemJdkUtil.resolveDependentJdk(projectSdk)
}


private fun findRegisteredSdk(sdk: Sdk): Sdk? = runReadAction {
  val projectJdkTable = ProjectJdkTable.getInstance()
  projectJdkTable.findJdk(sdk.name, sdk.sdkType.name)
}
