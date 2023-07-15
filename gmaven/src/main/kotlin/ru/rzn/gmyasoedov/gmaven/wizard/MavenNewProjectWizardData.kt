package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.externalSystem.service.project.wizard.MavenizedNewProjectWizardData
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.projectRoots.Sdk
import ru.rzn.gmyasoedov.serverapi.model.MavenProject

interface MavenNewProjectWizardData : MavenizedNewProjectWizardData<MavenProject> {

  val sdkProperty: GraphProperty<Sdk?>

  var sdk: Sdk?
}