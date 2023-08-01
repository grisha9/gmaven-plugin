package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.ide.JavaUiBundle
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.wizard.MavenizedNewProjectWizardStep
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.externalSystem.util.ui.DataView
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.DependentSdkType
import com.intellij.openapi.roots.ui.configuration.sdkComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.ValidationInfoBuilder
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import java.io.File
import javax.swing.Icon

abstract class GMavenNewProjectWizardStep<ParentStep>(parent: ParentStep) :
    MavenizedNewProjectWizardStep<MavenProject, ParentStep>(parent),
    MavenNewProjectWizardData
        where ParentStep : NewProjectWizardStep,
              ParentStep : NewProjectWizardBaseData {

    final override val sdkProperty = propertyGraph.property<Sdk?>(null)

    final override var sdk by sdkProperty

    protected fun setupJavaSdkUI(builder: Panel) {
        builder.row(JavaUiBundle.message("label.project.wizard.new.project.jdk")) {
            val sdkTypeFilter = { it: SdkTypeId -> it is JavaSdkType && it !is DependentSdkType }
            sdkComboBox(context, sdkProperty, StdModuleTypes.JAVA.id, sdkTypeFilter)
                .columns(COLUMNS_MEDIUM)
        }.bottomGap(BottomGap.SMALL)
    }

    override fun createView(data: MavenProject) = MavenDataView(data)

    override fun findAllParents(): List<MavenProject> {
        val project = context.project ?: return emptyList()
        val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, GMavenConstants.SYSTEM_ID)
        return projectsData.asSequence()
            .map { it.externalProjectStructure }
            .filterNotNull()
            .flatMap { ExternalSystemApiUtil.findAllRecursively(it, ProjectKeys.MODULE) }
            .map { it.data }
            .sortedBy { !MavenUtils.equalsPaths(it.linkedExternalProjectPath, parentStep.path) }
            .filter { it.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) is String }
            .map { toMavenProject(it) }
            .toList()
    }

    private fun toMavenProject(it: ModuleData): MavenProject =
        MavenProject.builder()
            .groupId(it.group!!)
            .artifactId(it.moduleName)
            .version(it.version!!)
            .file(File(it.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) as String))
            .basedir(it.linkedExternalProjectPath)
            .build()


    override fun ValidationInfoBuilder.validateGroupId(): ValidationInfo? {
        return validateCoordinates()
    }

    override fun ValidationInfoBuilder.validateArtifactId(): ValidationInfo? {
        return validateCoordinates()
    }

    private fun ValidationInfoBuilder.validateCoordinates(): ValidationInfo? {
        val mavenIds = parentsData.map { it.groupId to it.artifactId }.toSet()
        if (groupId to artifactId in mavenIds) {
            val message = ExternalSystemBundle.message(
                "external.system.mavenized.structure.wizard.entity.coordinates.already.exists.error",
                if (context.isCreatingNewProject) 1 else 0, "$groupId:$artifactId"
            )
            return error(message)
        }
        return null
    }

    class MavenDataView(override val data: MavenProject) : DataView<MavenProject>() {
        override val location: String = data.basedir
        override val icon: Icon = OpenapiIcons.RepositoryLibraryLogo
        override val presentationName: String = data.displayName
        override val groupId: String = data.groupId
        override val version: String = data.version

        override fun toString(): String {
            return "MavenDataView(data=$data, location='$location', presentationName='$presentationName', groupId='$groupId', version='$version')"
        }
    }
}