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
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectImportBuilder
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.nio.file.Path

class GOpenProjectProvider : AbstractOpenProjectProvider() {
    override val systemId: ProjectSystemId = GMavenConstants.SYSTEM_ID

    val builder: GMavenProjectBuilder
        get() = ProjectImportBuilder.EXTENSIONS_POINT_NAME.findExtensionOrFail(GMavenProjectBuilder::class.java)

    override fun isProjectFile(file: VirtualFile) = MavenUtils.isPomFile(null, file)

    override fun linkToExistingProject(projectFile: VirtualFile, project: Project) {
        val mavenSettings = MavenSettings.getInstance(project)
        mavenSettings.storeProjectFilesExternally = true
        val mavenProjectSettings = createMavenProjectSettings(projectFile, project)
        attachGradleProjectAndRefresh(mavenProjectSettings, project)
        //todo validate java home
    }

    private fun createMavenProjectSettings(projectFile: VirtualFile, project: Project): MavenProjectSettings {
        val projectDirectory = if (projectFile.isDirectory) projectFile else projectFile.parent
        val settings = MavenProjectSettings()
        settings.mavenHome = Path.of("/home/Grigoriy.Myasoedov/.sdkman/candidates/maven/3.8.5").toString()
        settings.externalProjectPath = projectFile.canonicalPath
        settings.projectDirectory = projectDirectory.canonicalPath
        settings.jdkPath = ExternalSystemJdkUtil.getJdk(project, ExternalSystemJdkUtil.USE_INTERNAL_JAVA)?.homePath
        return settings;
    }

    private fun attachGradleProjectAndRefresh(settings: ExternalProjectSettings, project: Project) {
        val internalJdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk()
        val externalProjectPath = settings.externalProjectPath
        val sdk = MavenUtils.suggestProjectSdk() ?: internalJdk
        val embeddedMavenPath =
            Path.of("/home/Grigoriy.Myasoedov/.sdkman/candidates/maven/3.8.5")//GMavenConstants.embeddedMavenPath.value
        /*val t = Thread({
            val processSupport = GServerRemoteProcessSupport(sdk, null, embeddedMavenPath)
            val server = processSupport.acquire(this, "", EmptyProgressIndicator())
            //server.getModel(GetModelRequest())
            processSupport.stopAll()
        })
        t.start()*/

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