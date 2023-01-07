// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.ide.JavaUiBundle
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.externalSystem.service.project.wizard.MavenizedNewProjectWizardStep
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.externalSystem.util.ui.DataView
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.DependentSdkType
import com.intellij.openapi.roots.ui.configuration.sdkComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.*
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectsManager
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import javax.swing.Icon

abstract class GMavenNewProjectWizardStep<ParentStep>(parent: ParentStep) :
  MavenizedNewProjectWizardStep<MavenProject, ParentStep>(parent)
  where ParentStep : NewProjectWizardStep,
        ParentStep : NewProjectWizardBaseData {

  // used externally
  @Suppress("MemberVisibilityCanBePrivate")
  val sdkProperty = propertyGraph.property<Sdk?>(null)

  val sdk by sdkProperty

  override fun setupSettingsUI(builder: Panel) {
    with(builder) {
      row(JavaUiBundle.message("label.project.wizard.new.project.jdk")) {
        val sdkTypeFilter = { it: SdkTypeId -> it is JavaSdkType && it !is DependentSdkType }
        sdkComboBox(context, sdkProperty, StdModuleTypes.JAVA.id, sdkTypeFilter)
          .columns(COLUMNS_MEDIUM)
      }
    }
    super.setupSettingsUI(builder)
  }

  override fun createView(data: MavenProject) = MavenDataView(data)

  override fun findAllParents(): List<MavenProject> {
    val project = context.project ?: return emptyList()
    val projectsManager = MavenProjectsManager.getInstance(project)
    return projectsManager.projects
  }

  override fun ValidationInfoBuilder.validateGroupId(): ValidationInfo? {
    return validateCoordinates()
  }

  override fun ValidationInfoBuilder.validateArtifactId(): ValidationInfo? {
    return validateCoordinates()
  }

  private fun ValidationInfoBuilder.validateCoordinates(): ValidationInfo? {
    val mavenIds = parentsData.map { it.groupId to it.artifactId }.toSet()
    if (groupId to artifactId in mavenIds) {
      val message = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.entity.coordinates.already.exists.error",
        if (context.isCreatingNewProject) 1 else 0, "$groupId:$artifactId")
      return error(message)
    }
    return null
  }

  class MavenDataView(override val data: MavenProject) : DataView<MavenProject>() {
    override val location: String = data.buildDirectory
    override val icon: Icon = OpenapiIcons.RepositoryLibraryLogo
    override val presentationName: String = data.displayName
    override val groupId: String = data.groupId
    override val version: String = data.version
  }
}