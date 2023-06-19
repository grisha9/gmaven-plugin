package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ExternalStorageConfigurationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.packaging.artifacts.ModifiableArtifactModel
import com.intellij.projectImport.ProjectImportBuilder
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import javax.swing.Icon

class GMavenProjectBuilder : ProjectImportBuilder<MavenProject>() {
    override fun getName(): String {
        return GMavenConstants.GMAVEN
    }

    override fun getIcon(): Icon {
        return OpenapiIcons.RepositoryLibraryLogo
    }

    override fun isMarked(element: MavenProject?): Boolean {
        return false
    }

    override fun setOpenProjectSettingsAfter(on: Boolean) {}

    override fun isSuitableSdkType(sdk: SdkTypeId): Boolean {
        return sdk === JavaSdk.getInstance()
    }


    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?,
        artifactModel: ModifiableArtifactModel?
    ): List<Module> {
        val isVeryNewProject =
            project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) === java.lang.Boolean.TRUE
        if (isVeryNewProject) {
            ExternalStorageConfigurationManager.getInstance(project).isEnabled = true
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            FileDocumentManager.getInstance().saveAllDocuments()
        }
        MavenUtils.setupProjectSdk(project)
        val basePath = project.basePath
        if (basePath == null) {
            MavenLog.LOG.warn("base path is null")
            return emptyList()
        }
        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
            ExternalSystemUtil.refreshProject(
                basePath,
                ImportSpecBuilder(project, GMavenConstants.SYSTEM_ID)
                    .callback(object : ExternalProjectRefreshCallback {
                        override fun onSuccess(
                            externalTaskId: ExternalSystemTaskId,
                            externalProject: DataNode<ProjectData>?
                        ) {
                            externalProject?.also {
                                ProjectDataManager.getInstance().importData(it, project, false)
                                updateMavenSettings(project, basePath)
                            }
                        }
                    })
            )
        }
        return emptyList()
    }

}