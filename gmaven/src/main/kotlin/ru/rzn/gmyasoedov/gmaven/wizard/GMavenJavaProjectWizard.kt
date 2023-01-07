package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizard
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizardData
import com.intellij.ide.projectWizard.generators.JavaNewProjectWizard
import com.intellij.ide.starters.local.StandardAssetsProvider
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.name
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.path
import com.intellij.ide.wizard.chain
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.GMAVEN
import ru.rzn.gmyasoedov.serverapi.model.MavenId
import java.nio.file.Path

class GMavenJavaProjectWizard : BuildSystemJavaNewProjectWizard {

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
            val builder = GMavenModuleBuilder().apply {
                moduleJdk = sdk
                name = parentStep.name
                contentEntryPath = Path.of(parentStep.path, parentStep.name).toString()

                parentProject = parentData
                aggregatorProject = parentData
                projectId = MavenId(groupId, artifactId, version)
                isInheritGroupId = parentData?.groupId == groupId
                isInheritVersion = parentData?.version == version
            }

            ExternalProjectsManagerImpl.setupCreatedProject(project)
            project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, true)

            builder.commit(project)
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