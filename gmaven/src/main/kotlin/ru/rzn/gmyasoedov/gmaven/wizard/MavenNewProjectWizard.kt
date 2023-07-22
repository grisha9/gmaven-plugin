package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizard
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizardData
import com.intellij.ide.projectWizard.generators.JavaNewProjectWizard
import com.intellij.ide.starters.local.StandardAssetsProvider
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.name
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.path
import com.intellij.ide.wizard.chain
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.GMAVEN
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenId
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class MavenNewProjectWizard : BuildSystemJavaNewProjectWizard {

    override val name = GMAVEN

    override fun createStep(parent: JavaNewProjectWizard.Step) = Step(parent).chain(::AssetsStep)

    class Step(parent: JavaNewProjectWizard.Step) :
        GMavenNewProjectWizardStep<JavaNewProjectWizard.Step>(parent),
        BuildSystemJavaNewProjectWizardData by parent {

        private val addSampleCodeProperty = propertyGraph.property(true)
            .bindBooleanStorage("NewProjectWizard.addSampleCodeState")

        var addSampleCode by addSampleCodeProperty

        override fun setupSettingsUI(builder: Panel) {
            super.setupSettingsUI(builder)
            builder.row {
                checkBox(UIBundle.message("label.project.wizard.new.project.add.sample.code"))
                    .bindSelected(addSampleCodeProperty)
            }.topGap(TopGap.SMALL)
        }

        override fun setupProject(project: Project) {
            super.setupProject(project)
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
                    val modulePath = buildFile.parent.toNioPath().absolutePathString()
                    val projectSettings = MavenSettings.getInstance(project)
                        .getLinkedProjectSettings(modulePath)
                        ?: throw ExternalSystemException("settings not found $modulePath")
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

    private class AssetsStep(private val parent: Step) : AssetsNewProjectWizardStep(parent) {
        override fun setupAssets(project: Project) {
            outputDirectory = Path.of(path, name).toString()
            addAssets(StandardAssetsProvider().getMavenIgnoreAssets())
            if (parent.addSampleCode) {
                withJavaSampleCodeAsset(Path.of("src", "main", "java").toString(), parent.groupId)
            }
        }
    }
}