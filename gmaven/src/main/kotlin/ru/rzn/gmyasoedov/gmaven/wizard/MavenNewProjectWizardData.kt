// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package ru.rzn.gmyasoedov.gmaven.wizard

import com.intellij.openapi.externalSystem.service.project.wizard.MavenizedNewProjectWizardData
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.projectRoots.Sdk
import ru.rzn.gmyasoedov.serverapi.model.MavenProject

interface MavenNewProjectWizardData : MavenizedNewProjectWizardData<MavenProject> {

  val sdkProperty: GraphProperty<Sdk?>

  var sdk: Sdk?
}