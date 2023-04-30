// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectImportBuilder
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings
import ru.rzn.gmyasoedov.gmaven.settings.DistributionType
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils

class GOpenProjectProvider : AbstractOpenProjectProvider() {
    override val systemId: ProjectSystemId = GMavenConstants.SYSTEM_ID

    val builder: GMavenProjectBuilder
        get() = ProjectImportBuilder.EXTENSIONS_POINT_NAME.findExtensionOrFail(GMavenProjectBuilder::class.java)

    override fun isProjectFile(file: VirtualFile) = MavenUtils.isPomFile(null, file)

    override fun linkToExistingProject(projectFile: VirtualFile, project: Project) {
        val mavenSettings = MavenSettings.getInstance(project)
        mavenSettings.storeProjectFilesExternally = true
        val mavenProjectSettings = createMavenProjectSettings(projectFile, project)
        attachProjectAndRefresh(mavenProjectSettings, project)
        //todo validate java home
    }

    private fun createMavenProjectSettings(projectFile: VirtualFile, project: Project): MavenProjectSettings {
        val projectDirectory = if (projectFile.isDirectory) projectFile else projectFile.parent
        val settings = MavenProjectSettings()
        settings.distributionSettings = getDistributionSettings(settings, project, projectDirectory)
        settings.externalProjectPath = projectFile.canonicalPath
        settings.projectDirectory = projectDirectory.canonicalPath
        settings.jdkName = (MavenUtils.suggestProjectSdk()
            ?: ExternalSystemJdkUtil.getJdk(project, ExternalSystemJdkUtil.USE_PROJECT_JDK))?.name
        return settings;
    }

    private fun getDistributionSettings(
        settings: MavenProjectSettings,
        project: Project,
        projectDirectory: VirtualFile
    ): DistributionSettings {
        if (settings.distributionSettings.type == DistributionType.CUSTOM) return settings.distributionSettings

        val distributionUrl = MvnDotProperties.getDistributionUrl(project, projectDirectory.path)
        if (distributionUrl.isNotEmpty()) return DistributionSettings.getWrapper(distributionUrl)

        val mavenHome = MavenUtils.resolveMavenHome()
        if (mavenHome != null)  return DistributionSettings.getLocal(mavenHome.toPath())

        return settings.distributionSettings
    }

    private fun attachProjectAndRefresh(settings: MavenProjectSettings, project: Project) {
        //val javaHome = ExternalSystemJdkUtil.getJavaHome()
        //val internalJdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk()
        //setupMavenJvm(project, settings)
        val externalProjectPath = settings.externalProjectPath


        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(settings)
        if (Registry.`is`("external.system.auto.import.disabled")) return
        ExternalSystemUtil.refreshProject(
            externalProjectPath,
            ImportSpecBuilder(project, SYSTEM_ID)
                .usePreviewMode()
                .use(ProgressExecutionMode.MODAL_SYNC)
        )

        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
            ExternalSystemUtil.refreshProject(
                externalProjectPath,
                ImportSpecBuilder(project, SYSTEM_ID)
                    .callback(createFinalImportCallback(project, externalProjectPath))
            )
        }
    }

    private fun createFinalImportCallback(
        project: Project,
        externalProjectPath: String
    ): ExternalProjectRefreshCallback {
        return object : ExternalProjectRefreshCallback {
            override fun onSuccess(externalProject: DataNode<ProjectData>?) {
                if (externalProject == null) return
                //selectDataToImport(project, externalProjectPath, externalProject)
                importData(project, externalProject)
                //updateGradleJvm(project, externalProjectPath)
            }
        }
    }

    /*private fun selectDataToImport(project: Project, externalProjectPath: String, externalProject: DataNode<ProjectData>) {
      val settings = GradleSettings.getInstance(project)
      val showSelectiveImportDialog = settings.showSelectiveImportDialogOnInitialImport()
      val application = ApplicationManager.getApplication()
      if (showSelectiveImportDialog && !application.isHeadlessEnvironment) {
        application.invokeAndWait {
          val projectInfo = InternalExternalProjectInfo(GSYSTEM_ID, externalProjectPath, externalProject)
          val dialog = ExternalProjectDataSelectorDialog(project, projectInfo)
          if (dialog.hasMultipleDataToSelect()) {
            dialog.showAndGet()
          }
          else {
            Disposer.dispose(dialog.disposable)
          }
        }
      }
    }*/

    private fun importData(project: Project, externalProject: DataNode<ProjectData>) {
        ProjectDataManager.getInstance().importData(externalProject, project, false)
    }
}