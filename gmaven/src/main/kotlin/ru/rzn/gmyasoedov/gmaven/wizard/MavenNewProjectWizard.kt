package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizard
import com.intellij.ide.projectWizard.generators.JavaNewProjectWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.GMAVEN
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenId
import java.nio.file.Path

class MavenNewProjectWizard : BuildSystemJavaNewProjectWizard {

    override val name = GMAVEN

    override fun createStep(parent: JavaNewProjectWizard.Step) =
        object : GMavenNewProjectWizardStep<JavaNewProjectWizard.Step>(parent) {
            override fun setupProject(project: Project) {
                project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, true)
                project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
                ExternalProjectsManagerImpl.setupCreatedProject(project)

                var buildFile: VirtualFile? = null
                runWriteAction {
                    val moduleDir = Path.of(parentStep.path, parentStep.name)
                    buildFile = GMavenModuleBuilderHelper.createExternalProjectConfigFile(moduleDir)
                }

                MavenUtils.runWhenInitialized(project) {
                    setupProjectFiles(buildFile!!, project)
                }
            }

            private fun setupProjectFiles(buildFile: VirtualFile, project: Project) {
                ApplicationManager.getApplication().invokeLater {
                    GMavenModuleBuilderHelper(
                        MavenId(groupId, artifactId, version), parentData,
                        parentData?.groupId == groupId, parentData?.version == version
                    ).setupBuildScript(project, sdk, buildFile)

                    if (context.isCreatingNewProject) {
                        val openProcessor = ProjectOpenProcessor.EXTENSION_POINT_NAME
                            .findExtensionOrFail(GProjectOpenProcessor::class.java)
                        openProcessor.importProjectAfterwards(project, buildFile)
                    } else {
                        val projectSettings = MavenSettings.getInstance(project)
                            .getLinkedProjectSettings(parentStep.path)
                            ?: throw ExternalSystemException("settings not found " + parentStep.path)
                        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
                            ExternalSystemUtil.refreshProject(
                                projectSettings.externalProjectPath,
                                ImportSpecBuilder(project, GMavenConstants.SYSTEM_ID)
                            )
                        }
                    }
                }
            }
        }
}
